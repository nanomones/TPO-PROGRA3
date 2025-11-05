package model;

public final class Cliente {
    public final String nombre;
    public final Perfil perfil;

    public Cliente(String nombre, Perfil perfil) {
        this.nombre = nombre;
        this.perfil = perfil;
    }
}
