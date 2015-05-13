package ru.rintd.json2grid;

import ru.rintd.json2grid.Building.InternLevel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.algorithm.CGAlgorithms;


/**
 * 
 * @author mag
 * Класс предназначен для задания параметров и методов преобразования геометрии в координаты грида и обратно
 */
public class GridTransformation {
	
	private double grid_h;
	public double xMin, xMax, yMin, yMax;
	
	/**
	 * Количество ячеек по оси <b>i</b>
	 * */
	public int iNum;
	/**
	 * Количество ячеек по оси <b>j</b>
	 * */
	public int jNum;
	
	private int iShift, jShift;
	
	public GridTransformation(double gridCell_h, Building bldVMjson) {
		grid_h = gridCell_h;
		//xMin = geomXMin; xMax = geomXMax; yMin = geomYMin; yMax = geomYMax;

		// Определить граничные размеры геометрии: Xmin,Xmax, Ymin,Ymax; NumZ;
		xMax = bldVMjson.Level[0].BuildElement[0].XY[0][0][0];
		xMin = bldVMjson.Level[0].BuildElement[0].XY[0][0][0];
		yMax = bldVMjson.Level[0].BuildElement[0].XY[0][0][1];
		yMin = bldVMjson.Level[0].BuildElement[0].XY[0][0][1];
		for( int i=0; i < bldVMjson.Level.length; i++) {	//по этажам
			for( int j=0; j < bldVMjson.Level[i].BuildElement.length; j++) {	//по помещениям на этаже
				for( int k=0; k < bldVMjson.Level[i].BuildElement[j].XY.length; k++) {	//по кольцам полигона одного помещения
					for( int l=0; l < bldVMjson.Level[i].BuildElement[j].XY[k].length; l++) {	//по всем точкам в кольце
						if(bldVMjson.Level[i].BuildElement[j].XY[k][l][0] < xMin) xMin = bldVMjson.Level[i].BuildElement[j].XY[k][l][0];
						if(bldVMjson.Level[i].BuildElement[j].XY[k][l][0] > xMax) xMax = bldVMjson.Level[i].BuildElement[j].XY[k][l][0];
						if(bldVMjson.Level[i].BuildElement[j].XY[k][l][1] < yMin) yMin = bldVMjson.Level[i].BuildElement[j].XY[k][l][1];
						if(bldVMjson.Level[i].BuildElement[j].XY[k][l][1] > yMax) yMax = bldVMjson.Level[i].BuildElement[j].XY[k][l][1];
					}
				}
			}
		}
		//System.out.println("Min-Max: " + xMin + "   " + xMax + "\n" + yMin + "   " + yMax); 
		
		// Определить параметры сетки: iNum, jNum, kNum = NumZ;  С добавлением граничных ячеек (минимум двух) исходя из GRID_H
		// с добавлением граничных ячеек (2*2=4), но также +1, так как привязываемся к центру ячейки
		iNum = (int)Math.round(Math.ceil((xMax - xMin)/grid_h)) + 1 + 4;
		jNum = (int)Math.round(Math.ceil((yMax - yMin)/grid_h)) + 1 + 4;
		
		// добавим граничные ячейки (по две с каждой границы) и определим сдвижки в сетки (//и в координатах) iShift(//xShift) и jShift(//yShift)
		iShift = (int)Math.round(xMin/grid_h) - 2;
		//double xShift = (iShift + 2) * GRID_H;
		jShift = (int)Math.round(yMin/grid_h) - 2;
		//double yShift = (jShift + 2) * GRID_H;
		
		// Определить параметры афинного преобразования x,y -> i,j и обратно
		// i = (int)Math.round(x/GRID_H) - iShift
		// x = (i + iShift) * GRID_H
		// j = (int)Math.round(y/GRID_H) - jShift
		// y = (j + jShift) * GRID_H
	}
	
	public GridEnvelope getEnvelope(BuildElement buildElement) {
		
		double xMax = buildElement.XY[0][0][0];
		double xMin = buildElement.XY[0][0][0];
		double yMax = buildElement.XY[0][0][1];
		double yMin = buildElement.XY[0][0][1];
		
		//Определим крайние координаты
		for( int k=0; k < buildElement.XY.length; k++) {	//по кольцам полигона одного помещения
			for( int l=0; l < buildElement.XY[k].length; l++) {	//по всем точкам в кольце
				if(buildElement.XY[k][l][0] < xMin) xMin = buildElement.XY[k][l][0];
				if(buildElement.XY[k][l][0] > xMax) xMax = buildElement.XY[k][l][0];
				if(buildElement.XY[k][l][1] < yMin) yMin = buildElement.XY[k][l][1];
				if(buildElement.XY[k][l][1] > yMax) yMax = buildElement.XY[k][l][1];
			}
		}
		//переструктурируем геометрию колец в Coordinate[][]
		Coordinate[][] geom = new Coordinate[buildElement.XY.length][];
		for(int k=0; k<buildElement.XY.length; k++) {
			geom[k] = new Coordinate[buildElement.XY[k].length];
			for(int l=0; l<buildElement.XY[k].length; l++) {
				geom[k][l] = new Coordinate(buildElement.XY[k][l][0], buildElement.XY[k][l][1]);
			}
		}
		return new GridEnvelope((int)Math.round(xMin/grid_h) - iShift, 
								(int)Math.round(xMax/grid_h) - iShift, 
								(int)Math.round(yMin/grid_h) - jShift, 
								(int)Math.round(yMax/grid_h) - jShift,
								geom );
	}
	
	/**
	 * !!! Deprecated. Проверяет, находится ли ячейка в полигоне, заданном кольцами полигона. Первое кольцо - внешнее.
	 * @param i
	 * @param j
	 * @param rings
	 * @return
	 */
	public boolean isCellInBuildElement(int i, int j, Coordinate[][] rings) {
		
		double x = (i + iShift) * grid_h;
		double y = (j + jShift) * grid_h;
		boolean testInnerRings = false;
		
		Coordinate testPoint = new Coordinate(x,y);
		
		//TODO Проверить верно ли что внешнее кольцо идет в списке первым
		for(int k=1; k<rings.length; k++) {
			if(CGAlgorithms.isPointInRing(testPoint, rings[k])) testInnerRings = true;
		}
			
		return CGAlgorithms.isPointInRing(testPoint, rings[0]) && !(testInnerRings);
	}
	
	/**
	 * !!! Deprecated. Проверяет, находится ли ячейка на краях колец полигона, заданного кольцами.
	 * @param i
	 * @param j
	 * @param rings
	 * @return
	 */
	public boolean isCellOnEdgeBuildElement(int i, int j, Coordinate[][] rings) {
		
		double x = (i + iShift) * grid_h;
		double y = (j + jShift) * grid_h;
		
		for(int k=0; k<rings.length; k++) {
			for(int l=1; l<rings[k].length; l++) {
				int i1 = (int)Math.round(rings[k][l-1].x/grid_h) - iShift;
				int j1 = (int)Math.round(rings[k][l-1].y/grid_h) - jShift;
				int i2 = (int)Math.round(rings[k][l].x/grid_h) - iShift;
				int j2 = (int)Math.round(rings[k][l].y/grid_h) - jShift;
				//отработаем попадание в целочисленный конверт
				if( (i-i1)*(i-i2) > 0 || (j-j1)*(j-j2) > 0 ) continue;
				//отработаем отдельно вертикальные и горизонтальные случаи
				if( (i1==i2 && i1==i) || (j1==j2 && j1==j) ) return true;
				//далее вредное условие. Заменено выше на целочисленное
				//if( ((x-rings[k][l].x)*(x-rings[k][l-1].x) > 0) || ((y-rings[k][l].y)*(y-rings[k][l-1].y) > 0) ) continue;
				double m = (rings[k][l].y - rings[k][l-1].y)/(rings[k][l].x - rings[k][l-1].x);
				double h = rings[k][l].y - m*rings[k][l].x;
				double yP = m*x + h;
				double xP = (y-h)/m;
				if( (Math.abs(xP - x) < grid_h/2) || Math.abs(yP - y) < grid_h/2 ) return true;
			}
		}
		return false;
	}
	
	//Можно реализовать алгоритм для последних двух методов, 
	//который проверяет входимость угловых точек каждой ячейки внутрь полигона
	//Такой алгоритм больше бы подошел для заполнения внутренностей комнат и лестничных клеток.
	//А текущий алгоритм больше подходит для дверей
	//
	//В идеале НЕ нужно отдельно искать принадлежность к бордюрам. Лучше реализовать две похожие друг на друга
	//функции заполнения внутренностей, одна из которых самая жесткая(все угловые точки ячейки должны быть внутри),
	//а другая самая мягкая(хотя бы одна из угловых точек ячейки внутри полигона)
	
	/**
	 * Первый метод - проверяет находится ли ячейка внутри полигона, не включая(не касается) границ/колец, в том числе внутренних.
	 * Первое кольцо - внешнее.
	 * Хорошо подходит для заполнения внутренностей комнат и лестничных площадок.
	 * @param i
	 * @param j
	 * @param rings
	 * @return
	 */
	public boolean isCellInPolygon(int i, int j, Coordinate[][] rings) {
		
		//TODO Проверить верно ли что внешнее кольцо идет в списке первым.
		//Да, действительно идет первым в списке. Проверено.
		for(int k=1; k<rings.length; k++) {
			if(isCellOnRingSharp(i, j, rings[k])) return false;	//флаг попадания НА внутренние кольца
		}
		return isCellInRing(i, j, rings[0]);	//Совсем ли внутри внешнего кольца и даже не на границе
	}
	
	/**
	 * Вторая функция - проверяет находится ли ячейка внутри полигона, включая(может касаться) границ/колец, в том числе внутренних.
	 * @param i
	 * @param j
	 * @param rings
	 * @return
	 */
	public boolean isCellOnPolygon(int i, int j, Coordinate[][] rings) {
		
		//TODO Проверить верно ли что внешнее кольцо идет в списке первым.
		//Да, действительно идет первым в списке. Проверено.
		for(int k=1; k<rings.length; k++) {
			if(isCellInRing(i, j, rings[k])) return false;	//флаг попадания ВНУТРЬ внутренних колец
		}
		return isCellOnRingSharp(i, j, rings[0]);	//Внутри ли внешнего кольца, возможно даже на границе
	}
	
//-----------------Private methods-----------------------------------
	
	//Проверяет находится ли точка с учетом размера ячейки целиком внутри кольца (даже не на границе)
	private boolean isCellInRing(int i, int j, Coordinate[] ring) {
		
		double x = (i + iShift) * grid_h;
		double y = (j + jShift) * grid_h;
		
		if(!CGAlgorithms.isPointInRing(new Coordinate(x-grid_h/2,y-grid_h/2), ring)) return false;
		if(!CGAlgorithms.isPointInRing(new Coordinate(x+grid_h/2,y-grid_h/2), ring)) return false;
		if(!CGAlgorithms.isPointInRing(new Coordinate(x-grid_h/2,y+grid_h/2), ring)) return false;
		if(!CGAlgorithms.isPointInRing(new Coordinate(x+grid_h/2,y+grid_h/2), ring)) return false;
		return true;
	}
	
	//!!! Deprecated.
	//Проверяет находится ли точка с учетом размера ячейки в кольце (включая границы)
	//Метод не работает, если линейные размеры кольца меньше размера ячейки. Это плохо для тонких дверей и столбов.
	//Поэтому реализован другой метод isCellOnRingSharp, использующий методы пересечения полигонов
	/*
	private boolean isCellOnRing(int i, int j, Coordinate[] ring) {
		
		double x = (i + iShift) * grid_h;
		double y = (j + jShift) * grid_h;
		
		if(CGAlgorithms.isPointInRing(new Coordinate(x-grid_h/2,y-grid_h/2), ring)) return true;
		if(CGAlgorithms.isPointInRing(new Coordinate(x+grid_h/2,y-grid_h/2), ring)) return true;
		if(CGAlgorithms.isPointInRing(new Coordinate(x-grid_h/2,y+grid_h/2), ring)) return true;
		if(CGAlgorithms.isPointInRing(new Coordinate(x+grid_h/2,y+grid_h/2), ring)) return true;
		return false;
	}
	*/
	
	//Проверяет находится ли точка с учетом размера ячейки в кольце (включая границы). Работает даже для ячеек больших чем размер геометрии колец
	//Метод работает даже на мелких кольцах (см.выше)
	private boolean isCellOnRingSharp(int i, int j, Coordinate[] ring) {
		
		double x = (i + iShift) * grid_h;
		double y = (j + jShift) * grid_h;
		
		//Создаем координаты из углов ячейки для полигона
		Coordinate[] cellCoords = new Coordinate[5];
		cellCoords[0] = new Coordinate(x-grid_h/2,y-grid_h/2);
		cellCoords[1] = new Coordinate(x+grid_h/2,y-grid_h/2);
		cellCoords[2] = new Coordinate(x-grid_h/2,y+grid_h/2);
		cellCoords[3] = new Coordinate(x+grid_h/2,y+grid_h/2);
		cellCoords[4] = new Coordinate(x-grid_h/2,y-grid_h/2);	//замыкаем
		
		GeometryFactory mGF = new GeometryFactory();
		GeometryFactory mGF1 = new GeometryFactory();
		Polygon mP = new Polygon( new LinearRing(new CoordinateArraySequence(cellCoords), mGF), null, mGF );	//полигон ячейки
		Polygon mP1 = new Polygon( new LinearRing(new CoordinateArraySequence(ring), mGF1), null, mGF1 );		//полигон кольца
		
		return  mP.intersects(mP1);
	}
	
	/**
	 * Возвращает массив элементов [этаж][полигон]
	 * @param building план
	 * @return полигоны
	 */
	public static Polygon[][] getJts(Building building){
		
		InternLevel[] level = building.Level;
		
		Polygon[][] geo = new Polygon[level.length][];
		int i = 0;
		for (InternLevel internLevel : level) {
			
			BuildElement[] buildElement = internLevel.BuildElement;
			geo[i] = new Polygon[buildElement.length];
			int j =0;
			for (BuildElement buildElement2 : buildElement) {
				Polygon pol = Json2Grid.xy2Polygon(buildElement2);
				geo[i][j] = pol;
				j++;
			}
			i++;
			
		}
		
		return geo;
	}
}
