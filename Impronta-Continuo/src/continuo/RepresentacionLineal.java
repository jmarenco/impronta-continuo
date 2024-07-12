package continuo;

import java.text.DecimalFormat;
import java.util.Arrays;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class RepresentacionLineal
{
	public static class Restriccion
	{
		public double ax;
		public double ay;
		public double b;
		
		@Override public String toString()
		{
			DecimalFormat df = new DecimalFormat("##0.00");
			return df.format(ax) + " x " + (ay > 0 ? "+ " : "- ") + df.format(Math.abs(ay)) + " y <= " + df.format(b);
		}
	}
	
	private Polygon _poligono;
	private Restriccion[] _restricciones;
	
	public RepresentacionLineal(Geometry geometry)
	{
		if( geometry instanceof Polygon )
		{
			_poligono = (Polygon)geometry;
			calcular();
		}
	}
	
	private void calcular()
	{
    	Coordinate[] c = _poligono.getCoordinates();
		_restricciones = new Restriccion[c.length-1];

		for(int i=0; i+1<c.length; ++i)
    		_restricciones[i] = obtenerRecta(c[i], c[i+1]);
    	
    	Point interior = _poligono.getInteriorPoint();
    	for(Restriccion restriccion: _restricciones)
    	{
    		acomodarSigno(restriccion, interior);
    		normalizar(restriccion);
    	}
	}
	
	private Restriccion obtenerRecta(Coordinate P, Coordinate Q)
	{
		Restriccion restriccion = new Restriccion();
		
	    restriccion.ax = Q.y - P.y; 
	    restriccion.ay = P.x - Q.x; 
	    restriccion.b = restriccion.ax * P.x + restriccion.ay * P.y;
	    
	    return restriccion;
	}
	
	private void acomodarSigno(Restriccion restriccion, Point punto)
	{
		double lhs = restriccion.ax * punto.getX() + restriccion.ay * punto.getY();

		if( lhs > restriccion.b )
		{
			restriccion.ax = -restriccion.ax;
			restriccion.ay = -restriccion.ay;
			restriccion.b = -restriccion.b;
		}
	}
	
	private void normalizar(Restriccion restriccion)
	{
		double menor = Double.POSITIVE_INFINITY;
		
		if( restriccion.ax != 0 )
			menor = Math.min(menor, Math.abs(restriccion.ax));
		
		if( restriccion.ay != 0 )
			menor = Math.min(menor, Math.abs(restriccion.ay));
		
		if( restriccion.b != 0 )
			menor = Math.min(menor, Math.abs(restriccion.b));
		
		if( menor != Double.POSITIVE_INFINITY )
		{
			restriccion.ax /= menor;
			restriccion.ay /= menor;
			restriccion.b /= menor;
		}
	}
	
	public int restricciones()
	{
		return _restricciones.length;
	}
	
	public Restriccion get(int i)
	{
		return _restricciones[i];
	}
	
	public double maxx()
	{
		return Arrays.stream(_poligono.getCoordinates()).mapToDouble(c -> c.x).max().getAsDouble();
	}
	public double minx()
	{
		return Arrays.stream(_poligono.getCoordinates()).mapToDouble(c -> c.x).min().getAsDouble();
	}
	public double maxy()
	{
		return Arrays.stream(_poligono.getCoordinates()).mapToDouble(c -> c.y).max().getAsDouble();
	}
	public double miny()
	{
		return Arrays.stream(_poligono.getCoordinates()).mapToDouble(c -> c.y).min().getAsDouble();
	}
}
