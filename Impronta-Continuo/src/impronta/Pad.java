package impronta;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class Pad
{
	// Datos privados
	private Instancia _instancia;
	private OGIP _ogip;
	private Semilla _semilla;
	private Coordinate _centroPad;
	private Coordinate _centroLocacion;
	
	// Extremos
	private Polygon _perimetro;
	private Polygon _locacion;
	
	// Constructor y setters privados, para los factories
	private Pad(Instancia instancia, Semilla semilla, Coordinate centro)
	{
		_instancia = instancia;
		_ogip = _instancia.getOGIP();
		_semilla = semilla;
		_centroPad = centro;
	}
	
	private void setPerimetro(Polygon perimetro)
	{
		_perimetro = perimetro;
	}
	private void setLocacion(Polygon locacion, Coordinate centroLocacion)
	{
		_locacion = locacion;
		_centroLocacion = centroLocacion;
	}
	
	// Factory para construir un pad seleccionando el centro de la locacion
	public static Pad flexible(Instancia instancia, Semilla semilla, Coordinate centro)
	{
		Pad pad = new Pad(instancia, semilla, centro);
		
		// Construye los puntos del perímetro
		Coordinate[] perimetro = new Coordinate[5];
		perimetro[0] = new Coordinate(centro.x - semilla.getLargo()/2, centro.y - semilla.getAncho()/2);
		perimetro[1] = new Coordinate(centro.x - semilla.getLargo()/2, centro.y + semilla.getAncho()/2);
		perimetro[2] = new Coordinate(centro.x + semilla.getLargo()/2, centro.y + semilla.getAncho()/2);
		perimetro[3] = new Coordinate(centro.x + semilla.getLargo()/2, centro.y - semilla.getAncho()/2);
		perimetro[4] = new Coordinate(centro.x - semilla.getLargo()/2, centro.y - semilla.getAncho()/2);
		
		pad.setPerimetro( pad.rotarPuntos(perimetro) );

		// Candidatos a centros de la locación
		Coordinate centroLocacion = new Coordinate((int)(centro.x - semilla.getLargo()/2 + semilla.getOffsetHorizontalLocacion()), (int)(centro.y - semilla.getAncho()/2 + semilla.getOffsetVerticalLocacion()));

		int tol = (int)semilla.getToleranciaLocacion();
		double raiz2 = Math.sqrt(2);

		ArrayList<Coordinate> centrosPosibles = new ArrayList<Coordinate>();
		centrosPosibles.add(new Coordinate(centroLocacion.x, centroLocacion.y));
		centrosPosibles.add(new Coordinate(centroLocacion.x + tol, centroLocacion.y));
		centrosPosibles.add(new Coordinate(centroLocacion.x + raiz2 * tol, centroLocacion.y + raiz2 * tol));
		centrosPosibles.add(new Coordinate(centroLocacion.x, centroLocacion.y + tol));
		centrosPosibles.add(new Coordinate(centroLocacion.x - raiz2 * tol, centroLocacion.y + raiz2 * tol));
		centrosPosibles.add(new Coordinate(centroLocacion.x - tol, centroLocacion.y + tol));
		centrosPosibles.add(new Coordinate(centroLocacion.x - raiz2 * tol, centroLocacion.y - raiz2 * tol));
		centrosPosibles.add(new Coordinate(centroLocacion.x, centroLocacion.y - tol));
		centrosPosibles.add(new Coordinate(centroLocacion.x + raiz2 * tol, centroLocacion.y - raiz2 * tol));
		centrosPosibles.add(new Coordinate(centroLocacion.x, centroLocacion.y));
		
		for(Coordinate c: centrosPosibles)
		{
			// Construye los puntos de la instalación
			Coordinate[] locacion = new Coordinate[5];
			locacion[0] = new Coordinate(c.x - semilla.getLargoLocacion()/2, c.y - semilla.getAnchoLocacion()/2);
			locacion[1] = new Coordinate(c.x - semilla.getLargoLocacion()/2, c.y + semilla.getAnchoLocacion()/2);
			locacion[2] = new Coordinate(c.x + semilla.getLargoLocacion()/2, c.y + semilla.getAnchoLocacion()/2);
			locacion[3] = new Coordinate(c.x + semilla.getLargoLocacion()/2, c.y - semilla.getAnchoLocacion()/2);
			locacion[4] = new Coordinate(c.x - semilla.getLargoLocacion()/2, c.y - semilla.getAnchoLocacion()/2);
			
			pad.setLocacion( pad.rotarPuntos(locacion), c );
			
			if( pad.locacionFactible() == true )
				break;
		}
		
		return pad;
	}
	
	// Factory para construir un pad sin considerar alternativas para el centro de la locacion
	public static Pad rigido(Instancia instancia, Semilla semilla, Coordinate centro)
	{
		Pad pad = new Pad(instancia, semilla, centro);
		
		// Construye los puntos del perímetro
		Coordinate[] perimetro = new Coordinate[5];
		perimetro[0] = new Coordinate(centro.x - semilla.getLargo()/2, centro.y - semilla.getAncho()/2);
		perimetro[1] = new Coordinate(centro.x - semilla.getLargo()/2, centro.y + semilla.getAncho()/2);
		perimetro[2] = new Coordinate(centro.x + semilla.getLargo()/2, centro.y + semilla.getAncho()/2);
		perimetro[3] = new Coordinate(centro.x + semilla.getLargo()/2, centro.y - semilla.getAncho()/2);
		perimetro[4] = new Coordinate(centro.x - semilla.getLargo()/2, centro.y - semilla.getAncho()/2);
		
		pad.setPerimetro( pad.rotarPuntos(perimetro) );

		// Centro de la locación
		Coordinate c = new Coordinate(centro.x - semilla.getLargo()/2 + semilla.getOffsetHorizontalLocacion(), centro.y - semilla.getAncho()/2 + semilla.getOffsetVerticalLocacion());

		// Construye los puntos de la instalación
		Coordinate[] locacion = new Coordinate[5];
		locacion[0] = new Coordinate(c.x - semilla.getLargoLocacion()/2, c.y - semilla.getAnchoLocacion()/2);
		locacion[1] = new Coordinate(c.x - semilla.getLargoLocacion()/2, c.y + semilla.getAnchoLocacion()/2);
		locacion[2] = new Coordinate(c.x + semilla.getLargoLocacion()/2, c.y + semilla.getAnchoLocacion()/2);
		locacion[3] = new Coordinate(c.x + semilla.getLargoLocacion()/2, c.y - semilla.getAnchoLocacion()/2);
		locacion[4] = new Coordinate(c.x - semilla.getLargoLocacion()/2, c.y - semilla.getAnchoLocacion()/2);
		
		pad.setLocacion( instancia.getFactory().createPolygon(locacion), c );
//		pad.setLocacion( pad.rotarPuntos(locacion), c );
		return pad;
	}
	
	// Rota los puntos alrededor del centro y construye un polígono con los puntos rotados
	private Polygon rotarPuntos(Coordinate[] puntos)
	{
		for(int i=0; i<puntos.length; ++i)
		{
			double[] pt = {puntos[i].x, puntos[i].y};
			AffineTransform.getRotateInstance(_instancia.getAngulo(), _centroPad.x, _centroPad.y).transform(pt, 0, pt, 0, 1);
			puntos[i] = new Coordinate(pt[0], pt[1]);
		}
		
		return _instancia.getFactory().createPolygon(puntos);
	}
	
	// Getters de las geometrías
	public Polygon getPerimetro()
	{
		return _perimetro;
	}
	public Polygon getLocacion()
	{
		return _locacion;
	}
	public Point getCentro()
	{
		return _instancia.getFactory().createPoint(_centroPad);
	}
	public Point getCentroLocacion()
	{
		return _instancia.getFactory().createPoint(_centroLocacion);
	}
	public Semilla getSemilla()
	{
		return _semilla;
	}
	
	// Obtiene las esquinas del perímetro
	public Coordinate[] getEsquinas()
	{
		return _perimetro.getCoordinates();
	}
	
	// Determina si el pad contiene al punto
	public boolean contiene(Point punto)
	{
		double offsetx = _centroPad.x < punto.getX() ? -0.01 : 0.01; 
		double offsety = _centroPad.y < punto.getY() ? -0.01 : 0.01;
		
		Coordinate desplazado = new Coordinate(punto.getX() - offsetx, punto.getY() - offsety);
		return _perimetro.contains(_instancia.getFactory().createPoint(desplazado));
//		return _perimetro.contains(punto);
	}
	public boolean contiene(Coordinate coordinate)
	{
		return contiene(_instancia.getFactory().createPoint(coordinate));
	}
	
	// Determina si los pads se intersecan
	public boolean interseca(Pad otro)
	{
		return _perimetro.intersects(otro._perimetro);
	}
	
	// Área del perímetro
	public double getArea()
	{
		return _perimetro.getArea();
	}
	
	// Determina si la locación se interseca con el área restringida
	private boolean interseca(Restriccion restriccion)
	{
		return restriccion.interseca(_locacion);
	}
	
	// Determina si el pad es factible
	public boolean factible(Region yacimiento)
	{
		return yacimiento.getGeometry().contains( this.getPerimetro() ) && yacimiento.getGeometry().contains( this.getLocacion() ) && this.locacionFactible();
	}
	public boolean factible(Geometry yacimiento)
	{
		return yacimiento.contains( this.getPerimetro() ) && yacimiento.contains( this.getLocacion() ) && this.locacionFactible();
	}
	public boolean factibleExceptoLocacion(Region yacimiento)
	{
		return yacimiento.getGeometry().contains( this.getPerimetro() ) && yacimiento.getGeometry().contains( this.getLocacion() ) && this.locacionFactible() == false;
	}
	
	// Determina si la locacion se interseca con algún área restringida
	public boolean locacionFactible()
	{
		for(Restriccion restriccion: _instancia.getRestricciones())
		{
			if( interseca(restriccion) )
				return false;
		}
		
		return true;		
	}
	
	public List<Restriccion> restriccionesConflictivas()
	{
		return _instancia.getRestricciones().stream().filter(r -> this.interseca(r)).collect(Collectors.toList());
	}
	
	// Valorizacion del pad
	public double getValorizacion()
	{
		double coeficiente = _semilla.getCoeficiente() > 0 ? _semilla.getCoeficiente() : 1.0;
		double valor = _ogip == null ? getArea() : _ogip.valor(_perimetro);

		return valor / coeficiente;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_centroLocacion == null) ? 0 : _centroLocacion.hashCode());
		result = prime * result + ((_centroPad == null) ? 0 : _centroPad.hashCode());
		result = prime * result + ((_semilla == null) ? 0 : _semilla.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		Pad otro = (Pad) obj;
		return _centroPad.equals(otro._centroPad) && _centroLocacion.equals(otro._centroLocacion) && _semilla.equals(otro._semilla);
	}
}
