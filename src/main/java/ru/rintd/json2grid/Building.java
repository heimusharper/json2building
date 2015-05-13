package ru.rintd.json2grid;

/**
 * Класс используется для десериализации стандарта VM.json Важно: подкласс BuildElement вынесен как внешний, т.к.
 * используется не только для десериализации.
 * 
 * @author mag
 */
public class Building {

    /** Общее название здания. */
    public String NameBuilding;
   
    /**
     * Структура с адресными данными <br>
     * StreetAddress - улица, дом<br>
     * City - город<br>
     * AddInfo - доп. информация<br>
     */
    public InternAddress Address;

    /** Массив этажей/уровней здания. */
    public InternLevel[] Level;

    /**
     * The Class InternAddress.
     */
    public class InternAddress {

        /** Улица, дом. */
        public String StreetAddress;

        /** Город. */
        public String City;

        /** Дополнительная информация. */
        public String AddInfo;
    }

    /**
     * The Class InternLevel.
     */
    public class InternLevel {

        /**
         * Название уровня.
         *
         * @Exapmle 3 этаж
         */
        public String NameLevel;

        /** Высота уровня над землей, м. */
        public double ZLevel;

        /** Массив помещений на данном этаже. */
        public BuildElement[] BuildElement;

        /**
         * Сенсорные узлы.
         *
         * @author harper
         */
        public Node[] nodes = new Node[0];
    }
}
