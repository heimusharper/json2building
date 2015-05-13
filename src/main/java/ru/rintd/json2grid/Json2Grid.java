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
