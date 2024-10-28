package com.hz.model.annotation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.AnnotationVisitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class AddAnnotationClassVisitor extends ClassVisitor {

    private final String annotationDesc;
    private final String channel;
    private List<String> words = new ArrayList<>();
    private boolean isEnumClass = false;
    public AddAnnotationClassVisitor(ClassVisitor classVisitor, String annotationDesc,String channel) {
        super(Opcodes.ASM9, classVisitor);
        this.annotationDesc = annotationDesc;
        this.channel = channel;
        words.clear();
        getWords();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        isEnumClass = (access & Opcodes.ACC_ENUM) != 0;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        System.out.println("visitField: " + name + ","+(access & Opcodes.ACC_SYNTHETIC));
        //如果是枚举类的字段。
        if (isEnumClass){
            return super.visitField(access, name, descriptor, signature, value);
        }
//        // 跳过 synthetic 字段
//        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
//            return super.visitField(access, name, descriptor, signature, value);
//        }

        FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
//        System.out.println("visitField fv: " +fv);
        return new FieldVisitor(Opcodes.ASM9, fv) {
            private boolean hasSerializedName = false; // 标记是否已存在 SerializedName 注解
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
//                System.out.println("visitAnnotation: "+desc);
                if (desc.equals(annotationDesc)) {
                    hasSerializedName = true; // 已存在 SerializedName 注解
                }
                return super.visitAnnotation(desc, visible);
            }

            @Override
            public void visitEnd() {
//                System.out.println("visitEnd: "+!hasSerializedName);
                // 检查当前字段是否已经有该注解
                if (!hasSerializedName) {
                    // 为 @SerializedName 添加名称，可以使用字段名或自定义名称
                    AnnotationVisitor av = fv.visitAnnotation(annotationDesc, true);
                    int num = sha256ToNonNegativeInt(name+"*"+channel);
                    int idx = words.isEmpty()?0:num%words.size();
                    String newName = words.isEmpty()? name : words.get(idx);
                    System.out.println("visitEnd name: "+name+", new :"+newName);
                    av.visit("value", newName); // 这里将字段名作为 SerializedName 的值
                    av.visitEnd(); // 结束注解访问
                }
                super.visitEnd();
            }
        };
    }

    public static int sha256ToNonNegativeInt(String input) {
        try {
            // 获取 SHA-256 的实例
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 计算输入字符串的哈希值，返回 byte 数组
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // 将前四个字节（32 位）转换为 int 类型
            int result = ((hash[0] & 0xFF) << 24) |
                    ((hash[1] & 0xFF) << 16) |
                    ((hash[2] & 0xFF) << 8) |
                    (hash[3] & 0xFF);
            // 使用按位与操作符确保非负
            return result & 0x7FFFFFFF;  // 强制将最高位（符号位）设置为 0 以确保非负数
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private void getWords(){
        // 获取类加载器
        ClassLoader classLoader = getClass().getClassLoader();
        // 从 resources 中读取 txt 文件
        InputStream inputStream = classLoader.getResourceAsStream("words.txt");
        if (inputStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 输出每行内容
                    words.add(line);
                }
            } catch (IOException e) {
                System.out.println("Error reading txt file");
            }
        } else {
            System.out.println("Failed to find the txt file");
        }
    }
}
