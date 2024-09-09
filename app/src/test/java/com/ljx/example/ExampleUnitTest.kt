package com.ljx.example

import org.junit.Test

import org.junit.Assert.*
import test.TestUtil

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
        val content = "public class B implements Serializable {\n" +
                "    private String e ;\n" +
                "    private Product product;\n" +
                "    private List<test.Product> productList;\n" +
                "    public B(String e) {\n" +
                "        this.e = e;\n" +
                "    }\n" +
                "\n" +
                "    public String getE() {\n" +
                "        return e;\n" +
                "    }\n" +
                "\n" +
                "    public void setE(String e) {\n" +
                "        this.e = e;\n" +
                "    }\n" +
                "\n" +
                "    public Product getProduct() {\n" +
                "        return product;\n" +
                "    }\n" +
                "\n" +
                "    public void setProduct(Product product) {\n" +
                "        this.product = product;\n" +
                "    }\n" +
                "\n" +
                "    public List<test.Product> getProductList() {\n" +
                "        return productList;\n" +
                "    }\n" +
                "\n" +
                "    public void setProductList(List<test.Product> productList) {\n" +
                "        this.productList = productList;\n" +
                "    }\n" +
                "\n" +
                "    public Product getPr() {\n" +
                "        return pr;\n" +
                "    }\n" +
                "\n" +
                "    public void setPr(Product pr) {\n" +
                "        this.pr = pr;\n" +
                "    }\n" +
                "\n" +
                "    public static class Product implements Serializable{\n" +
                "        private String id;\n" +
                "        private String name;\n" +
                "\n" +
                "        public String getName() {\n" +
                "            return name;\n" +
                "        }\n" +
                "\n" +
                "        public void setName(String name) {\n" +
                "            this.name = name;\n" +
                "        }\n" +
                "\n" +
                "        public String getId() {\n" +
                "            return id;\n" +
                "        }\n" +
                "\n" +
                "        public void setId(String id) {\n" +
                "            this.id = id;\n" +
                "        }\n" +
                "    }\n" +
                "    private Product pr;\n" +
                "}"
        val oldClassName = "Product"
        val newClassName = "AB"
        TestUtil.match(content,oldClassName,newClassName)
    }
}