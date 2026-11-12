package com.deldaran.politics;

public interface Obstacles {
    boolean bloque(double x, double y, double z, double rayon);
    
    default boolean surPont(double x, double y, double z) {
        return false;
    }

}
