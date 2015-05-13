package ru.rintd.json2grid;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.math.Vector2D;

/**
 * Класс, являющийся составной частью класса VMjson. Но выделенный отдельно для последующего использования.
 * @author mag
 *
 */
public class BuildElement {
	
	/**
	 * Наименование здания.
	 * */
	public String Name;
	
	/**
	 * Идетнификатор здания в формате UUID. <br><hr>
	 * UUID представляет собой 16-байтный (128-битный) номер. <br>
	 * @Example В шестнадцатеричной системе счисления UUID выглядит как:<br>
	 * 550e8400-e29b-41d4-a716-446655440000
	 * */
	public String Id;
	
	/**
	 * Тип пространства. Может принимать следующие значения:
	 * <ul>
	 * 	<li>Room - комната</li>
	 * 	<li>Staircase - лестница</li>
	 * 	<li>Outside - нерасчетное пространство</li>
	 * 	<li>DoorWayOut - дверь, выходящая наружу</li>
	 * 	<li>DoorWayInt - дверь, внутри здания</li>
	 * 	<li>DoorWay - проем</li>
	 * </ul>
	 * */
	public String Sign;	//Sign =  {Room, Staircase,  Outside, DoorWayOut,  DoorWayInt,  DoorWay} 
	
	/**
	 * Высота потолка помещения
	 * */
	public double SizeZ;
	
	/**
	 * Ширина. Применимо для двери и проемов
	 */
	public double Width;
	
	/**
	 * Тип помещения. Цифровой идентификатор вида пространства.
	 * <ol>
	 * 	<li>Жилые помещения гостиниц, общежитий и т. д.</li>
	 * 	<li>Столовая, буфет, зал ресторана</li>
	 * 	<li>Зал театра, кинотеатра, клуба, цирка</li>
	 * 	<li>Гардероб</li>
	 * 	<li>Хранилища библиотек, архивы</li>
	 * 	<li>Музеи, выставки</li>
	 * 	<li>Подсобные и бытовые помещения, лестничная клетка</li>
	 * 	<li>Административные помещения, учебные классы школ, ВУЗов, кабинеты поликлиник</li>
	 * 	<li>Магазины</li>
	 * 	<li>Зал вокзала</li>
	 * 	<li>Стоянки легковых автомобилей</li>
	 * 	<li>Стоянки легковых автомобилей (с двухуровневым хранением)</li>
	 * 	<li>Стадионы</li>
	 * 	<li>Спортзалы</li>
	 * 	<li>Торговый зал гипермаркета</li>
	 * </ol>
	 * */
	public int Type;
	
	/**
	 * Количество людей в помещении
	 * */
	public int NumPeople;
	
	/**
	 * Определяет наличие сценария для комнаты.<br>
	 * Если > 0, то в помещении возгорания.
	 * */
	public int SignScenario;
	
	/**
	 * Примечание. Дополнительная информация.
	 * */
	public String Note;
	
	/**
	 * Ссылки (UUID) на связанные помещения
	 * */
	public String[] Output;
	
	/**
	 * Геометрия.<br>
	 * [кольца][точки][0,1]
	 * */
	public double[][][] XY;
	
	
	/**
	 * Приватный метод, возвращающий полигон на структуре XY 
	 */
	private Polygon getPolygon() {
		
		if (XY == null) return null;	//если геометрии нет, то и в полигон не превратить
		GeometryFactory mGF = new GeometryFactory();
		//переструктурируем геометрию колец в Coordinate[][]
		//внешнее кольцо
		Coordinate[] geomOut = new Coordinate[XY[0].length];
		for(int l=0; l<XY[0].length; l++) {
			geomOut[l] = new Coordinate(XY[0][l][0], XY[0][l][1]);
		}		
		//внутренние кольца
		LinearRing[] internalRings = null;
		Coordinate[][] geomInt = null;
		if (XY.length >= 2) {	//если внутренние кольца есть
			geomInt = new Coordinate[XY.length - 1][];
			internalRings = new LinearRing[XY.length - 1];
			for (int k = 1; k < XY.length; k++) { //Начиная с первого кольца (не с нулевого)
				geomInt[k - 1] = new Coordinate[XY[k].length];
				for (int l = 0; l < XY[k].length; l++) {
					geomInt[k - 1][l] = new Coordinate(XY[k][l][0], XY[k][l][1]);
				}
				internalRings[k-1] = new LinearRing( new CoordinateArraySequence(geomInt[k-1]), mGF);
			}//for
		}//if
		
		return new Polygon( new LinearRing(new CoordinateArraySequence(geomOut), mGF), internalRings, mGF );
	}
	
	/**
	 * Метод возвращает площадь полигона данного элемента
	 * @return
	 */
	public double getArea() {
		
		Polygon mP = getPolygon();
		if(mP != null)
			return mP.getArea();
		else
			return 0;
	}

	/**
	 * Метод, возвращающий периметр
	 */
	public double getPerimeter() {
		
		Polygon mP = getPolygon();
		if(mP != null)
			return mP.getLength();
		else
			return 0;
	}
	
	
	/**
	 * Метод возвращает ширину двери
	 * */
	public double getWidth() {
		
		if (Width != 0) return Width;
		if(Sign.equals("DoorWayOut") || Sign.equals("DoorWayInt") || Sign.equals("DoorWay")) {
			//Если это дверь
			//половина модуля суммы векторов диагоналей (большая из двух вариантов)
			Vector2D v1 = new Vector2D(XY[0][0][0]-XY[0][2][0], XY[0][0][1]-XY[0][2][1]);
			Vector2D v2 = new Vector2D(XY[0][1][0]-XY[0][3][0], XY[0][1][1]-XY[0][3][1]);
			return Math.max(v1.add(v2).length(), v1.negate().add(v2).length()) / 2;
		} else {
			//Если не дверь
			return 0;
		}
	}
	
	/**
	 * Метод, возвращающий характерный больший размер (длину как-бы прямоугольника)
	 */
	public double getWidthMax() {
		
		double s = getArea();
		double p = getPerimeter();
		//далее параметры для решения квадратного уравнения
		double a = 2;
		double b = -p;
		double c = 2*s;
		//дискримининант
		double d = (b*b) - (4*a*c);
		//результат
		return (-b + Math.sqrt(d))/(2*a);
	}

	/**
	 * Метод, возвращающий характерный меньший размер (ширину как-бы прямоугольника)
	 */
	public double getWidthMin() {
		
		double s = getArea();
		double p = getPerimeter();
		//далее параметры для решения квадратного уравнения
		double a = 2;
		double b = -p;
		double c = 2*s;
		//дискримининант
		double d = (b*b) - (4*a*c);
		//результат
		return (-b - Math.sqrt(d))/(2*a);
	}
	
}
