package ru.rintd.json2grid;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * 
 * @author mag
 * Класс предназначен для представления прямоугольного ПОДгрида в большом гриде.
 * Содержит крайние индексы ПОДгрида, а также геометрию полигона
 */
public class GridEnvelope {
	
	public int iMin, iMax, jMin, jMax;
	public Coordinate[][] geom;
	
	public GridEnvelope(int i1, int i2, int j1, int j2, Coordinate[][] rings) {
		iMin = Math.min(i1, i2); iMax = Math.max(i1, i2); 
		jMin = Math.min(j1, j2); jMax = Math.max(j1, j2);
		geom = rings;
	}
	
	public int iSize() {
		return Math.abs(iMax - iMin) + 1;
	}
	
	public int jSize() {
		return Math.abs(jMax - jMin) + 1;
	}
}
