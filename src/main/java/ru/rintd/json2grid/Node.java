package ru.rintd.json2grid;

import java.util.Arrays;

/**
 * Узел.
 *
 * @author harper
 */
public class Node {

    /** xy - координаты узла. */
    public double[] xy;

    /** Высота узла над полом (если не указывать, то равно 3). */
    public double h;
    /**
     * id комнаты/двери/пр.
     */
    public String buildElID;
    /**
     * Тип элемента. надо придумать.
     */
    public int type;

    /** id элемента. выдается приложением установки датчиков
     * по сути - физический идентификатар привязанной железки в 
     * уже действующей сети */
    public String uid;
    
    /**
     * Instantiates a new node.
     *
     * @param buildElemId ид элемента, к которому привязан
     * @param type тип узла
     * @param x координата x
     * @param y координата y
     * @param h высота над уровнем пола
     */
    public Node(String buildElemId, int type, double x, double y, double h) {

        this.buildElID = buildElemId;
        this.type = type;
        this.xy = new double[2];
        this.xy[0] = x;
        this.xy[1] = y;
        this.h = h;
        this.uid = ""+this.buildElID.hashCode();

    }

    /**
     * Instantiates a new node.
     *
     * @param buildElemId ид элемента, к которому привязан
     * @param type тип узла
     * @param x координата x
     * @param y координата y
     */
    public Node(String buildElemId, int type, double x, double y) {
        this.buildElID = buildElemId;
        this.type = type;
        this.xy = new double[2];
        this.xy[0] = x;
        this.xy[1] = y;
        this.h = 3;
        this.uid = null;
    }

    /**
     * Генератор идентификатора узла, пока так.
     *
     * @return идентификатор узла
     */
    // TODO генератор идентификатора узла
    /*public String genUID() {
        return String.valueOf(this.hashCode());
    }*/

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((buildElID == null) ? 0 : buildElID.hashCode());
        result = prime * result + Arrays.hashCode(xy);
        long temp;
        temp = Double.doubleToLongBits(h);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + type;
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Node other = (Node) obj;
        if (buildElID == null) {
            if (other.buildElID != null) return false;
        } else if (!buildElID.equals(other.buildElID)) return false;
        if (!Arrays.equals(xy, other.xy)) return false;
        if (Double.doubleToLongBits(h) != Double.doubleToLongBits(other.h)) return false;
        if (type != other.type) return false;
        return true;
    }

    public boolean equalsUID(Object obj){
        Node other = (Node) obj;
        if (uid == null) {
            if (other.uid != null) return false;
        } else if (!uid.equals(other.uid)) return false;
        return true;
    }
    
}
