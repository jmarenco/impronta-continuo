package testing;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import impronta.OGIP;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class DrawingPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	private static class Elemento
	{
		public Geometry geometry;
		public Color backColor;
		public Color foreColor;
		public boolean dashed;
	}
	
	private List<Elemento> geometries = new ArrayList<Elemento>();
	private OGIP ogip = null;
  
    private double _minx = Double.POSITIVE_INFINITY;
    private double _maxx = Double.NEGATIVE_INFINITY;
    private double _miny = Double.POSITIVE_INFINITY;
    private double _maxy = Double.NEGATIVE_INFINITY;
    private int _margen = 20;

    public void clear()
    {
    	geometries.clear();
    }
    
    public void addGeometry(Geometry geom)
    {
    	addGeometry(geom, Color.BLACK, null, false);
    }
    public void addGeometry(Geometry geom, Color fore)
    {
    	addGeometry(geom, fore, null, false);
    }
    public void addGeometry(Geometry geom, Color fore, Color back, boolean dash)
    {
    	Elemento elemento = new Elemento();
    	elemento.geometry = geom;
    	elemento.backColor = back;
    	elemento.foreColor = fore;
    	elemento.dashed = dash;
    	
        geometries.add(elemento);
        
        for(Coordinate c: geom.getCoordinates())
        {
        	_minx = Math.min(_minx, c.x);
        	_maxx = Math.max(_maxx, c.x);
        	_miny = Math.min(_miny, c.y);
        	_maxy = Math.max(_maxy, c.y);
        }
    }
    
    public void addOGIP(OGIP obj)
    {
    	ogip = obj;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (!geometries.isEmpty())
        {
            for (Elemento elemento: geometries)
            {
            	if( elemento.geometry.getClass().getName().contains("Polygon") )
            		drawPolygon((Polygon)elemento.geometry, elemento.foreColor, elemento.backColor, elemento.dashed, g2d);

            	if( elemento.geometry.getClass().getName().contains("MultiPoint") )
            		drawMultiPoint((MultiPoint)elemento.geometry, elemento.foreColor, g2d);

            	if( elemento.geometry.getClass().getName().contains("LineString") )
            		drawLineString((LineString)elemento.geometry, elemento.foreColor, g2d);

            	if( elemento.geometry.getClass().getName().contains(".Point") )
            		drawPoint((Point)elemento.geometry, elemento.foreColor, g2d);
            }
        }
        
        if( ogip != null )
        	drawOGIP(g2d);
    }

    private void drawPolygon(Polygon polygon, Color fore, Color back, boolean dashed, Graphics2D g2d)
    {
    	java.awt.Polygon p = new java.awt.Polygon();
    	
    	for(Coordinate c: polygon.getCoordinates())
    		p.addPoint(convx(c), convy(c));

    	Stroke original = g2d.getStroke();

    	if( back != null )
    	{
	    	g2d.setColor(back);
	    	g2d.fill(p);
    	}
    	
    	if( dashed == true )
    	{
        	Stroke dash = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {3}, 0);
        	g2d.setStroke(dash);
    	}
    	
    	g2d.setColor(fore);
    	g2d.draw(p);
    	g2d.setStroke(original);
    }

    private void drawLineString(LineString polygon, Color fore, Graphics2D g2d)
    {
    	g2d.setColor(fore);
    	Coordinate[] c = polygon.getCoordinates();

    	for(int i=0; i+1<c.length; ++i)
    		g2d.drawLine(convx(c[i]), convy(c[i]), convx(c[i+1]), convy(c[i+1]));
    }

    private void drawMultiPoint(MultiPoint points, Color fore, Graphics2D g2d)
    {
    	for(Coordinate c: points.getCoordinates())
    	{
    		Ellipse2D.Double circle = new Ellipse2D.Double(convx(c), convy(c), 2, 2);
        	g2d.setColor(fore);
    		g2d.fill(circle);
    	}
    }

    private void drawPoint(Point point, Color fore, Graphics2D g2d)
    {
    	double radio = fore == Color.BLUE ? 1 : 3;
 		Ellipse2D.Double circle = new Ellipse2D.Double(convx(point.getCoordinate()) - radio, convy(point.getCoordinate()) - radio, 2 * radio, 2 * radio);
    	g2d.setColor(fore);
   		g2d.fill(circle);
    }
    
    private void drawOGIP(Graphics2D g2d)
    {
    	double min = ogip.minMedicion();
    	double max = ogip.maxMedicion();
    	
    	if( max <= min )
    		return;
    	
    	for(Coordinate c: ogip.getPuntos())
    	{
    		int nivel = 255 - (int)((ogip.getValor(c) - min) * 255 / (max - min));
    		g2d.setColor(new Color(nivel, nivel, nivel));
    		
    		Ellipse2D.Double circle = new Ellipse2D.Double(convx(c), convy(c), 3, 3);
    		g2d.fill(circle);
    	}
    }

    private int convx(Coordinate c)
    {
    	return (int)(_margen + (c.x - _minx) * (getWidth() - 2*_margen) / (_maxx - _minx));
    }
    
    private int convy(Coordinate c)
    {
    	return (int)(-_margen + getHeight() - (c.y - _miny) * (getHeight() - 2*_margen) / (_maxy - _miny));
    }
}