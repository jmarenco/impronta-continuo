package impronta;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;

// Representa una discretización de la región
public class Discretizacion
{
	// Instancia y región asociadas
	private Instancia _instancia;
	private Geometry _yacimiento;
	
	// Centroide y longitud al punto más distante
	private Point _centroide;
	private double _radio;
	
	// Puntos de la discretización
	private MultiPoint _puntos;
	
	// Cache: Puntos de la discretizacion como Points
	private Point[] _asPoints;
	
	// Genera la discretización
	public Discretizacion(Instancia instancia)
	{
		_instancia = instancia;
		_yacimiento = instancia.getRegion().getGeometry();
	
		calcularRadio();

		ArrayList<Coordinate> cartesianos = generarPuntos();
		MultiPoint rotados = rotarPuntos(cartesianos);
		Geometry puntos = _yacimiento.intersection(rotados);
		
		if( puntos.getClass().getName().equals( rotados.getClass().getName()) == false )
			throw new RuntimeException("Error! La intersección de la región con un MultiPoint no retornó un MultiPoint. Se recibió: " + puntos.getClass().getName() );
		
		_puntos = (MultiPoint)puntos;
		_asPoints = null;
	}
	
	// Calcula la distancia del centroide al punto más lejano
	public void calcularRadio()
	{
		_radio = 0;
		_centroide = _yacimiento.getCentroid();

		for(Coordinate c: _yacimiento.getCoordinates())
			_radio = Math.max(_radio,  _centroide.getCoordinate().distance(c));
	}
	
	// Genera puntos alineados con los ejes en un área que contiene a la región
	private ArrayList<Coordinate> generarPuntos()
	{
		ArrayList<Coordinate> ret = new ArrayList<Coordinate>();
		
		for(double x=_centroide.getX()-_radio; x<_centroide.getX()+_radio; x+=_instancia.getPasoHorizontal())
		for(double y=_centroide.getY()-_radio; y<_centroide.getY()+_radio; y+=_instancia.getPasoVertical())
			ret.add(new Coordinate(x, y));
		
		return ret;		
	}
	
	// Rota los puntos alrededor del centroide
	public MultiPoint rotarPuntos(ArrayList<Coordinate> puntos)
	{
		Coordinate[] coordinates = new Coordinate[puntos.size()];

		int i = 0;
		for(Coordinate c: puntos)
		{
			double[] pt = {c.x, c.y};
			AffineTransform.getRotateInstance(_instancia.getAngulo(), _centroide.getX(), _centroide.getY()).transform(pt, 0, pt, 0, 1);

			coordinates[i] = new Coordinate(pt[0], pt[1]);
			++i;
		}
		
		return _yacimiento.getFactory().createMultiPoint(coordinates);
	}
	
	// Obtiene los puntos
	public MultiPoint getPuntos()
	{
		return _puntos;
	}
	
	// Obtiene los puntos como points
	public Point[] getPoints()
	{
		if( _asPoints == null )
		{
			Coordinate[] coordinates = _puntos.getCoordinates();
			_asPoints = new Point[coordinates.length];
			
			for(int i=0; i<coordinates.length; ++i)
				_asPoints[i] = _puntos.getFactory().createPoint(coordinates[i]);
		}
			
		return _asPoints;
	}
	
	// Construye pads centrados en los puntos de la discretización
	public ArrayList<Pad> construirPads()
	{
		ArrayList<Pad> ret = new ArrayList<Pad>();
	
		for(Semilla s: _instancia.getSemillas())
		for(Coordinate c: _puntos.getCoordinates())
		{
			Pad pad = Pad.flexible(_instancia, s, c);
			if( _yacimiento.contains( pad.getPerimetro() ) && _yacimiento.contains( pad.getLocacion() ) && pad.factible() )
				ret.add(pad);
		}
		
		return ret;
	}
}
