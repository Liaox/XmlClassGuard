package test;

import java.util.ArrayList;
import java.util.List;

public class A {

    private List<B> e = new ArrayList<>();

    public List<B> getE() {
        return e;
    }

    public void setE(List<B> e) {
        this.e = e;
    }

    public void c(test.B... l){
        F f = new F<B>() {
            @Override
            public void onResult(B b) {
                A.B h = new A.B();
                h.getC();
            }
        };
    }

    public static class B{
        private String c;

        public String getC() {
            return c;
        }

        public void setC(String c) {
            this.c = c;
        }
    }
}
