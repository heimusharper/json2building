package ru.rintd.json2grid;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * Класс выполняет преобразование структуры vmjson в численную квази-трехмерную (поэтажную) сетку для дальнейшего
 * моделирования задач эвакуации Каждая ячейка решетки должна содержать тип, и ссылку на пространственный элемент
 * (BuildElement) геометрии
 * 
 * @author mag
 */
public class Json2Grid {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        demo("src/main/resources/vmtest1.json");
        Building building = getStructure(args[0]);
        for (int i = 0; i < building.Level[0].BuildElement.length; i++) {
            System.out.println("Ширина: " + building.Level[0].BuildElement[i].getWidth());
        }
    }

    /**
     * Метод demo(), демонстрирующий использование класса VMjson2Grid.
     * 
     * @param jsonfile
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void demo(String jsonfile) throws FileNotFoundException, IOException {

        /**
         * Демонстрация создания грида с шагом 0.1 на основе файла json
         */
        System.out.println("Proccessing file: " + jsonfile);

        /**
         * Прочитаем файл *.json Статичный метод VMjson2Grid.getVMjson("filename.json") возвращает структуру к класс
         * VMjson
         */
        Building building = getStructure(jsonfile);

        /**
         * Обрежем двери по границам соседних комнат
         */
        cutAllDoors(building);
        
        /**
         * Зададим размер ячейки будущего грида
         */
        final float GRID_H = 0.1f;	// шаг сетки

        /**
         * Создаем объект с параметрами и методами дальнейших трансформаций
         */
        GridTransformation gridTrans = new GridTransformation(GRID_H, building);

        /**
         * Статичный метод VMjson2Grid.makeGrid создает сетку Ячейки, не являющиеся помещениями, лестничными площадками
         * или дверями - пустые, то есть null
         */
        GridCell[][][] grid = makeGrid(building, gridTrans);

        /**
         * Размеры грида
         */
        System.out.println("Размеры Грида i-j-k: " + gridTrans.iNum + "-" + gridTrans.jNum + "-"
                + building.Level.length);

        /**
         * Распечатка грида
         */
        printGrid(grid);
    }

    /**
     * Метод, возвращающий сетку(грид) на основе струкуры VMjson и объекта GridTransformation Ячейки, представляющие
     * собой помещения, лестничные площадки или двери, содержат объекты GridCell c типом(GridCell.type) и ссылкой на
     * BuildElement. Остальные ячейки - null.
     * 
     * @param bldVMjson
     * @param gridTrans
     * @return
     */
    public static GridCell[][][] makeGrid(Building bldVMjson, GridTransformation gridTrans) {

        // Размеры грида
        int iNum = gridTrans.iNum;
        int jNum = gridTrans.jNum;
        int kNum = bldVMjson.Level.length;

        // Создать пустую сетку (с объектами null)
        GridCell[][][] myGrid = new GridCell[kNum][jNum][iNum];
        // System.out.println("Размеры Грида " + iNum + "-" + jNum + "-" + kNum);

        // Создадим ассоциативный массив HashMap<id,bldElement>
        HashMap<String, BuildElement> bldLinks = new HashMap<String, BuildElement>();
        for (int l = 0; l < bldVMjson.Level.length; l++) {	// по этажам
            for (int r = 0; r < bldVMjson.Level[l].BuildElement.length; r++) {	// по помещениям на этаже
                bldLinks.put(bldVMjson.Level[l].BuildElement[r].Id, bldVMjson.Level[l].BuildElement[r]);
            }
        }

        // Для каждой комнаты и лестничной клетки:
        for (int l = 0; l < bldVMjson.Level.length; l++) {	// по этажам
            for (int r = 0; r < bldVMjson.Level[l].BuildElement.length; r++) {	// по помещениям на этаже
                // выберем комнаты и лестничные клетки
                if (!bldVMjson.Level[l].BuildElement[r].Sign.equals("Room")
                        && !bldVMjson.Level[l].BuildElement[r].Sign.equals("Staircase")) continue;
                // Определить Envelope(I,J) для текущего BuildElement
                GridEnvelope ge = gridTrans.getEnvelope(bldVMjson.Level[l].BuildElement[r]);
                // Для каждой ячейки в Envelope(I,J)
                for (int i = ge.iMin; i <= ge.iMax; i++) {
                    for (int j = ge.jMin; j <= ge.jMax; j++) {
                        // Проверить входит ли i,j во внутренность полигона (НЕ на границах). Если входит, то поставить
                        // type & link
                        if (gridTrans.isCellInPolygon(i, j, ge.geom)) {
                            if (myGrid[l][j][i] == null) myGrid[l][j][i] = new GridCell();
                            myGrid[l][j][i].type = (bldVMjson.Level[l].BuildElement[r].Sign.equals("Room")) ? GridCell.ROOM
                                    : GridCell.STAIRCASE;
                            myGrid[l][j][i].buildElement = bldVMjson.Level[l].BuildElement[r];
                        }
                    }
                }// обход ячеек в Envelope
            }// по помещениям на этаже
        }// по этажам

        // Для каждой двери
        for (int l = 0; l < bldVMjson.Level.length; l++) {	// по этажам
            for (int r = 0; r < bldVMjson.Level[l].BuildElement.length; r++) {	// по помещениям на этаже
                // выберем двери
                if (!bldVMjson.Level[l].BuildElement[r].Sign.equals("DoorWay")
                        && !bldVMjson.Level[l].BuildElement[r].Sign.equals("DoorWayInt")
                        && !bldVMjson.Level[l].BuildElement[r].Sign.equals("DoorWayOut")) continue;
                // Не будем обрабатывать межэтажные DoorWay
                if (bldVMjson.Level[l].BuildElement[r].Sign.equals("DoorWay")
                        && bldLinks.get(bldVMjson.Level[l].BuildElement[r].Output[0]).Sign.equals("Staircase")
                        && bldLinks.get(bldVMjson.Level[l].BuildElement[r].Output[1]).Sign.equals("Staircase")) continue;
                // Определить Envelope(I,J) для текущего BuildElement
                GridEnvelope ge = gridTrans.getEnvelope(bldVMjson.Level[l].BuildElement[r]);
                // Для каждой ячейки в Envelope(I,J)
                for (int i = ge.iMin; i <= ge.iMax; i++) {
                    for (int j = ge.jMin; j <= ge.jMax; j++) {
                        // Проверить входит ли i,j во внутренность полигона (включая границы). Если входит, то поставить
                        // type & link
                        if (gridTrans.isCellOnPolygon(i, j, ge.geom)) {
                            if (myGrid[l][j][i] == null) myGrid[l][j][i] = new GridCell();
                            if (bldVMjson.Level[l].BuildElement[r].Sign.equals("DoorWay")) myGrid[l][j][i].type = GridCell.DOORWAY;
                            if (bldVMjson.Level[l].BuildElement[r].Sign.equals("DoorWayInt")) myGrid[l][j][i].type = GridCell.DOORWAYINT;
                            if (bldVMjson.Level[l].BuildElement[r].Sign.equals("DoorWayOut")) myGrid[l][j][i].type = GridCell.DOORWAYOUT;
                            myGrid[l][j][i].buildElement = bldVMjson.Level[l].BuildElement[r];
                        }
                    }
                }// обход ячеек в Envelope
            }// по помещениям на этаже
        }// по этажам

        return myGrid;
    }

    public static void usage() {
        System.out.println("--------- Usage: ---------------------------------------------------------");
        System.out.println("VMjson bldVMjson = VMjson2Grid.getVMjson(\"jsonfile.json\");");
        System.out.println("GridTransformation gridTrans = new GridTransformation(GRID_H, bldVMjson);");
        System.out.println("GridCell[][][] myGrid = VMjson2Grid.makeGrid(bldVMjson, gridTrans);");
        System.out.println("--------------------------------------------------------------------------");
    }

    public static Building getStructure(String fileName) throws FileNotFoundException, IOException {
        // Десериализуем файл json В объект VMjson
        Gson gson = new Gson();
        FileInputStream file = new FileInputStream(fileName);
        BufferedReader in = new BufferedReader(new InputStreamReader(file));
        Building building = gson.fromJson(in, Building.class);
        file.close();
        return building;
    }

    public static void saveVMjson(String fileName, Building json){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileOutputStream file;
		try {
			file = new FileOutputStream(fileName);
	        String outer = gson.toJson(json, Building.class);
	        file.write(outer.getBytes());
	        file.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * Распечатка грида по этажам для проверки. Важно - распечатка вверх ногами, потому что Y-геометрия растет вверх
     * 
     * @param grid
     */
    public static void printGrid(GridCell[][][] grid) {

        for (int k = 0; k < grid.length; k++) {				// по уровням
            for (int j = 0; j < grid[k].length; j++) {			// по вертикальным ячейкам
                for (int i = 0; i < grid[k][j].length; i++) {	// по горизонтальным ячейкам
                    if (grid[k][j][i] != null) System.out.print(grid[k][j][i].type);
                    else System.out.print(".");	// если null
                }
                System.out.println("");
            }
            System.out.println("\n\n");	// следующий уровень
        }
    }

    /**
     * Добавить элемент в Node сенсор.
     */
    public static void addInNode(ArrayList<Node>[] n, Building json) {
        if (n.length == json.Level.length) for (int i = 0; i < json.Level.length; i++)
            json.Level[i].nodes = (Node[]) (n[i].toArray());
    }
    
    //
    //Методы, необходимые для Обрезки дверей
    //

	public static Polygon xy2Polygon(double[][][] xy) {
		
		if (xy == null) return null;	//если геометрии нет
		GeometryFactory mGF = new GeometryFactory();
		//переструктурируем геометрию внешнего кольца в Coordinate[]
		Coordinate[] geomOut = new Coordinate[xy[0].length];
		for(int l=0; l<xy[0].length; l++) {
			geomOut[l] = new Coordinate(xy[0][l][0], xy[0][l][1]);
		}	
		return new Polygon( new LinearRing(new CoordinateArraySequence(geomOut), mGF), null, mGF );
	}
	
	/**
	 * превращение координат типа xy[][][] в поигоны с отверсиями + в getUserData хранится исходный BuildElement
	 * @param BuildElement
	 * @return
	 */
public static Polygon xy2Polygon(BuildElement be) {
		
	
	double[][][] xy = be.XY;
		if (xy == null) return null;	//если геометрии нет
		GeometryFactory mGF = new GeometryFactory();
		//переструктурируем геометрию внешнего кольца в Coordinate[]
		Coordinate[] geomOut = new Coordinate[xy[0].length];
		// bash
		for(int l=0; l<xy[0].length; l++) {
			geomOut[l] = new Coordinate(xy[0][l][0], xy[0][l][1]);
		}	
		// hole
		LinearRing[] lr = new LinearRing[xy.length-1];
		for (int l = 0; l < xy.length-1; l++){
			Coordinate[] g = new Coordinate[xy[l+1].length];
			for (int i = 0; i < xy[l+1].length; i++){
				g[i] = new Coordinate(xy[l+1][i][0], xy[l+1][i][1]);
			}
			lr[l] = new LinearRing(new CoordinateArraySequence(g), mGF);
		}
		Polygon pol = new Polygon( new LinearRing(new CoordinateArraySequence(geomOut), mGF), lr, mGF );
		pol.setUserData(be);
		return pol;
	}
	
	public static double[][][] polygon2XY(Polygon polygon) {
		
		Coordinate[] coords = polygon.getCoordinates();
		double[][][] xy = new double[1][coords.length][2];
		for(int i=0; i<coords.length; i++) {
			xy[0][i][0] = coords[i].x;
			xy[0][i][1] = coords[i].y;
		}
		return xy;
	}
	
	public static void cutDoor(BuildElement door, BuildElement room) {
		
		Polygon pol1 = xy2Polygon(door.XY);
		Polygon pol2 = xy2Polygon(room.XY);
		try {
			Polygon geomResult = (Polygon) pol1.difference(pol2);
			double [][][] tmp_door = polygon2XY(geomResult);
			if (tmp_door.length == 0 || tmp_door[0].length == 0) return;
			door.XY  = tmp_door;
		} catch (Exception e) {}
	}
	
	/**
	 * Метод, режущий двери по границам соседних комнат
	 * @param building
	 */
	public static void cutAllDoors(Building building) {

		// Создадим ассоциативный массив HashMap<id,bldElement>
        HashMap<String, BuildElement> bldLinks = new HashMap<String, BuildElement>();
        // Создадим ассоциативный массив HashMap<id,levelNum> для определения этажности
        HashMap<String, Integer> bldLevel = new HashMap<String, Integer>();
        for (int l = 0; l < building.Level.length; l++) {	// по этажам
            for (int r = 0; r < building.Level[l].BuildElement.length; r++) {	// по помещениям на этаже
                bldLinks.put(building.Level[l].BuildElement[r].Id, building.Level[l].BuildElement[r]);
                bldLevel.put(building.Level[l].BuildElement[r].Id, l);
            }
        }
		//для каждого этажа в здании
		for(int k=0; k < building.Level.length; k++) {
			//для каждой двери на этаже
			for(int i=0; i < building.Level[k].BuildElement.length; i++) {
				BuildElement be = building.Level[k].BuildElement[i];
				//выбираем только двери, исключая межэтажные проемы
				if( be.Sign.contains("Door") && !( be.Sign.equals("DoorWay") && be.Output.length > 1 && (bldLevel.get(be.Output[0]) != bldLevel.get(be.Output[1])) ) ) {
					//для каждой комнаты или лестничной клетки, соседствующей с дверью
					for(int r=0; r < be.Output.length; r++) {
						cutDoor(be, bldLinks.get(be.Output[r]));
					}
				}
			}
		}
	}
    
}
