package cl.duoc.bancoxyz.core.model;

public class Cuenta {
    private Long cuentaId;
    private String nombreTitular;
    private Long saldoContable;
    private Integer edadTitular;
    private String tipoCuenta;
    private Long lineaSobregiro;
    private Double tasaInteresAnual;
    private String estado;

    public Cuenta() {}

    public Cuenta(Long cuentaId, String nombreTitular, Long saldoContable, Integer edadTitular,
                  String tipoCuenta, Long lineaSobregiro, Double tasaInteresAnual, String estado) {
        this.cuentaId = cuentaId;
        this.nombreTitular = nombreTitular;
        this.saldoContable = saldoContable;
        this.edadTitular = edadTitular;
        this.tipoCuenta = tipoCuenta;
        this.lineaSobregiro = lineaSobregiro;
        this.tasaInteresAnual = tasaInteresAnual;
        this.estado = estado;
    }

    public Long getCuentaId() { return cuentaId; }
    public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }

    public String getNombreTitular() { return nombreTitular; }
    public void setNombreTitular(String nombreTitular) { this.nombreTitular = nombreTitular; }

    public Long getSaldoContable() { return saldoContable; }
    public void setSaldoContable(Long saldoContable) { this.saldoContable = saldoContable; }

    public Integer getEdadTitular() { return edadTitular; }
    public void setEdadTitular(Integer edadTitular) { this.edadTitular = edadTitular; }

    public String getTipoCuenta() { return tipoCuenta; }
    public void setTipoCuenta(String tipoCuenta) { this.tipoCuenta = tipoCuenta; }

    public Long getLineaSobregiro() { return lineaSobregiro; }
    public void setLineaSobregiro(Long lineaSobregiro) { this.lineaSobregiro = lineaSobregiro; }

    public Double getTasaInteresAnual() { return tasaInteresAnual; }
    public void setTasaInteresAnual(Double tasaInteresAnual) { this.tasaInteresAnual = tasaInteresAnual; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public String toString() {
        return "Cuenta{" +
                "cuentaId=" + cuentaId +
                ", nombreTitular='" + nombreTitular + '\'' +
                ", saldoContable=" + saldoContable +
                ", edadTitular=" + edadTitular +
                ", tipoCuenta='" + tipoCuenta + '\'' +
                ", lineaSobregiro=" + lineaSobregiro +
                ", tasaInteresAnual=" + tasaInteresAnual +
                ", estado='" + estado + '\'' +
                '}';
    }
}
