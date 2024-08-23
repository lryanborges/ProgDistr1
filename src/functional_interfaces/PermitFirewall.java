package functional_interfaces;

@FunctionalInterface
public interface PermitFirewall {
    void permit(String ip);
}
