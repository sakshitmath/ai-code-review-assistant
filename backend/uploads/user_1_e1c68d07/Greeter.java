public class Greeter {
    private String name;

    public void setName(String n) {
        name = n;
    }

    public void greet() {
        System.out.println("Hi " + name);
    }
}