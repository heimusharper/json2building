package ru.rintd.json2grid;

/**
 * Класс для объектов, находящихся в ячейках грида/сетки
 * На основе данного класса следует создавать дочерние с добавлением необходимых полей
 * @author mag
 *
 */
public class GridCell {
	public static final int OUTSIDE = -1;	//нерасчетная область
	public static final int WALL = 0;		//стены
	public static final int DOORWAYOUT = 1;	//дверь наружу (ВЫХОД)
	public static final int DOORWAYINT = 2;	//внутренняя дверь
	public static final int DOORWAY = 2;	//проем между помещениями
	public static final int ROOM = 5;		//помещения
	public static final int STAIRCASE = 7;	//лестничная площадка
	
	public int type;	// Почти как BuildElement.Sign =  { Room, Staircase,  Outside, DoorWayOut,  DoorWayInt,  DoorWay} 
	public BuildElement buildElement;
}
