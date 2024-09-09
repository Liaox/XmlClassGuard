package test;

import java.io.Serializable;
import java.util.List;

public class B implements Serializable {
    private String e ;
    private Product product;
    private List<test.Product> productList;
    public B(String e) {
        this.e = e;
    }

    public String getE() {
        return e;
    }

    public void setE(String e) {
        this.e = e;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public List<test.Product> getProductList() {
        return productList;
    }

    public void setProductList(List<test.Product> productList) {
        this.productList = productList;
    }

    public Product getPr() {
        return pr;
    }

    public void setPr(Product pr) {
        this.pr = pr;
    }

    public static class Product implements Serializable{
        private String id;
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
    private Product pr;
}
