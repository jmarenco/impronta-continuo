package testing;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import colgen.CplexCG;
import colgen.Dualizer;
import colgen.SolverCG;
import colgen.SolverCplex;
import continuo.Modelo;
import continuo.SolverContinuo;
import ilog.cplex.IloCplex;
import impronta.Instancia;
import impronta.Pad;
import impronta.Restriccion;
import impronta.Solucion;

public class Interfaz
{
    public static void main(String[] args) throws Exception
    {
		System.out.println("UFO Continuo - 0.71");
		
		Interfaz interfaz = new Interfaz("Instancias/pol.1s.07.xml");
		interfaz.resolverContinuo(20);
    }
    
    public Interfaz(String archivo)
    {
    	_instancia = construirInstancia(archivo);
    }
    
    private Instancia _instancia;
    private SolverCG _solverCG;
    private SolverContinuo _solverContinuo;
    private SolverCplex _solverCplex;
    private Solucion _solucion;
    private JFrame _framePrincipal;
    private DrawingPanel _panelPrincipal;    
    private JFrame _frameDualizer;
    private DrawingPanel _panelDualizer;    
    
	private void resolverCG(double paso, boolean raiz)
	{
		_instancia.setPasoHorizontal(paso);
		_instancia.setPasoVertical(paso);
    	
        _solverCG = new SolverCG(_instancia);
        _solverCG.iniciar();

    	crearVentana();
    	crearDualizer();

    	int iteraciones = 0;
        while( _solverCG.iterar() == true && iteraciones < 1500 )
        {
        	imprimirResumen();
        	++iteraciones;

        	if( _solverCG.getCplex().funcionObjetivo() == 840 || _solverCG.getCplex().cantidadVariables() % 10 == 0)
        	{
	        	limpiar(_panelPrincipal);
	            mostrarInstancia(_panelPrincipal);
	        	mostrarRestricciones();
	        	mostrarRelajacion();
	        	mostrarDuales(Duales.positivos);
	        	mostrarVioladorDual();
	        	mostrar(_framePrincipal);

	        	limpiar(_panelDualizer);
	            mostrarInstancia(_panelDualizer);
        		mostrarInputDualizer();
        		mostrarSolucionDualizer();
           		mostrarPadNoFactible();
            	mostrar(_frameDualizer);

//            	if( _solverCG.getCplex().funcionObjetivo() == 840 )
//            		new java.util.Scanner(System.in).nextInt();
        	}
        }
        
        if( raiz == false )
        {
	        resolverCplex();
        	limpiar(_panelPrincipal);
            mostrarInstancia(_panelPrincipal);
	        mostrarSolucion();
	    	mostrar(_framePrincipal);
        }
        else
        {
        	imprimirResumen();

        	limpiar(_panelPrincipal);
            mostrarInstancia(_panelPrincipal);
        	mostrarRestricciones();
        	mostrarRelajacion();
        	mostrarDuales(Duales.positivos);
        	mostrar(_framePrincipal);
        	
        	limpiar(_panelDualizer);
            mostrarInstancia(_panelDualizer);
    		mostrarInputDualizer();
    		mostrarSolucionDualizer();
       		mostrarPadNoFactible();
        	mostrar(_frameDualizer);
        }
        
        System.out.println();
        System.out.println("fobj = " + _solverCG.getCplex().funcionObjetivo());
	}

	private void resolverContinuo(int pads)
	{
		long inicio = System.currentTimeMillis();
    	
        _solverContinuo = new SolverContinuo(_instancia);
        _solverContinuo.setEliminacionSimetrias(true);
        _solverContinuo.setCutPool(true);
        _solverContinuo.setCortesDinamicos(true);
        _solverContinuo.setPads(pads);
        _solverContinuo.setObjetivo(Modelo.Objetivo.Cantidad);
        _solverContinuo.setTiempoMaximo(30);

        _solucion = _solverContinuo.resolver();
        
        long fin = System.currentTimeMillis();
        System.out.println(" -> Tiempo total: " + (fin - inicio) / 1000.0 + " sg.");

    	crearVentana();
        mostrarInstancia(_panelPrincipal);
        mostrarSolucion();
        mostrar(_framePrincipal);
	}
	
	private void resolverCplex()
	{
		if( _solverCG == null )
			return;
		
		Set<Pad> pads = _solverCG.getCplex().primales().keySet();
		Set<Point> points = _solverCG.getCplex().duales().keySet();
		
		_solverCplex = new SolverCplex(_instancia, pads, points);
		_solucion = _solverCplex.resolver();
	}
	
	private Instancia construirInstancia(String archivo)
	{
    	Instancia._formato = Instancia.Formato.French;
    	Instancia._alinear = true;
		Instancia instancia = new Instancia(archivo);
    	
		return instancia;
	}

	private void mostrarInstancia(DrawingPanel panel)
	{
		if( panel == null || _instancia == null )
			return;
		
        for(Polygon envolvente: _instancia.getRegion().getEnvolventes())
        	panel.addGeometry(envolvente);
        
        for(Polygon agujero: _instancia.getRegion().getAgujeros())
        	panel.addGeometry(agujero);
        
        for(Restriccion restriccion: _instancia.getRestricciones())
        	panel.addGeometry(restriccion.getPolygon(), Color.BLACK, Color.LIGHT_GRAY, true);
	}
	
	private void mostrarOGIP()
	{
        _panelPrincipal.addOGIP(_instancia.getOGIP());
	}
	
	private void mostrarDiscretizacion()
	{
		if( _panelPrincipal == null || _solverCG == null )
			return;
		
		System.out.println(_solverCG.getDiscretizacion().getPuntos().getCoordinates().length + " puntos generados");
       	_panelPrincipal.addGeometry(_solverCG.getDiscretizacion().getPuntos());
	}

	private void mostrarSolucion()
	{
		if( _panelPrincipal == null || _solucion == null )
			return;
		
        for(Pad pad: _solucion.getPads())
        {
            _panelPrincipal.addGeometry(pad.getPerimetro());
            _panelPrincipal.addGeometry(pad.getLocacion(), Color.BLACK, null, true);
            _panelPrincipal.addGeometry(pad.getCentro(), Color.RED);
        }
        
     	DecimalFormat df = new DecimalFormat("##0.00");
        System.out.println(" -> Pads: " + _solucion.getPads().size());
        System.out.println(" -> Area cubierta: " + df.format(_solucion.porcentajeCubierto()) + " %");
	}

	private void imprimirRelajacion()
	{
		if( _solverCG == null )
			return;
		
		Map<Pad, Double> solucion = _solverCG.primales();
        for(Pad pad: solucion.keySet())
        	System.out.println(" -> Pad (" + pad.getCentro() + ") = " + solucion.get(pad));
        
    	System.out.println(" -> " + _solverCG.getCplex().cantidadVariables() + " pads activos");
        System.out.println();
	}

	private void mostrarRelajacion()
	{
		if( _panelPrincipal == null || _solverCG == null )
			return;
		
		Map<Pad, Double> solucion = _solverCG.primales();
		
		// Ordena los pads por los valores de sus variables
		ArrayList<Pad> pads = new ArrayList<Pad>(solucion.keySet());
		Collections.sort(pads, (i,j) -> (int)Math.signum(solucion.get(i) - solucion.get(j)));
		
        for(Pad pad: pads)
        {
        	int rgbNum = 255 - (int)(255.0 * solucion.get(pad));
        	Color color = new Color(rgbNum,rgbNum,rgbNum);

            _panelPrincipal.addGeometry(pad.getPerimetro(), color);
        }
	}

	private enum Duales { todos, positivos };
	
	private void mostrarDuales(Duales modalidad)
	{
		if( _panelPrincipal == null || _solverCG == null )
			return;
		
		Map<Point, Double> duales = _solverCG.duales();
    	double max = duales.values().stream().max(Double::compare).get();
    	
		// Ordena los puntos por los valores de sus variables duales
		ArrayList<Point> points = new ArrayList<Point>(duales.keySet());
		Collections.sort(points, (i,j) -> (int)Math.signum(duales.get(i) - duales.get(j)));
		
		for(Point point: points)
        {
        	if( duales.get(point) < 0.01 && modalidad == Duales.positivos )
        		continue;
        	
        	int rgbNum = (int)(255.0 * (max - duales.get(point)) / max);
        	Color color = new Color(rgbNum,rgbNum,rgbNum);
        	
            _panelPrincipal.addGeometry(point, color);
        }
	}

	private void mostrarRestricciones()
	{
		if( _panelPrincipal == null || _solverCG == null )
			return;
		
        for(Point point: _solverCG.duales().keySet())
            _panelPrincipal.addGeometry(point, Color.BLUE);
	}

	private void mostrarVioladorDual()
	{
		if( _panelPrincipal == null || _solverCG == null )
			return;
		
		Pad pad = _solverCG.violadorDual();
		
		if( pad != null )
			_panelPrincipal.addGeometry(pad.getPerimetro(), Color.RED);
	}

	private void mostrarInputDualizer()
	{
		if( _panelDualizer == null || _solverCG == null )
			return;

        for(Pad pad: _solverCG.getDualizer().getPads())
        	_panelDualizer.addGeometry(pad.getPerimetro(), Color.WHITE);
        
        for(Point point: _solverCG.getDualizer().getPuntos())
        	_panelDualizer.addGeometry(point, Color.BLUE);
	}

	private void mostrarPadNoFactible()
	{
		if( _panelDualizer == null || _solverCG == null || _solverCG.getDualizer().getPadNoFactible() == null )
			return;

       	_panelDualizer.addGeometry(_solverCG.getDualizer().getPadNoFactible().getPerimetro(), Color.RED);
	}
	
	private void mostrarSolucionDualizer()
	{
		if( _panelDualizer == null || _solverCG == null )
			return;
		
		Map<Point, Double> vars = _solverCG.getDualizer().getXVars();
    	double max = vars.values().stream().max(Double::compare).get();
    	
        for(Point point: vars.keySet()) if( vars.get(point) > 0.01 )
        {
        	int rgbNum = (int)(255.0 * (max - vars.get(point)) / max);
        	Color color = new Color(rgbNum,rgbNum,rgbNum);
        	
            _panelDualizer.addGeometry(point, color);
        }
	}
	
	private void imprimirDuales()
	{
		if( _solverCG == null )
			return;
		
		Map<Point, Double> duales = _solverCG.duales();
        for(Point point: duales.keySet())
        	System.out.println(" -> Dual (" + point + ") = " + duales.get(point));
        
        System.out.println();
	}
	
	private void imprimirResumen()
	{
		CplexCG cplex = _solverCG.getCplex();
		Dualizer dualizer = _solverCG.getDualizer();
		DecimalFormat format = new DecimalFormat("###0.00");
		
		System.out.print(" -> It " + _solverCG.getIteracion() + ": ");
		
		if( dualizer != null )
		{
			System.out.print("DS = " + (dualizer.getCplexStatus() == IloCplex.Status.Optimal ? "Opt" : "***"));
			System.out.print(", dobj = " + format.format(dualizer.getObjetivo()));
			System.out.print(_solverCG.esDualOptimo() ? " *" : "  ");
		}
		
		System.out.print(" - Primal: ");
		System.out.print(cplex.cantidadVariables() + " vars; ");
		System.out.print(cplex.cantidadRestricciones() + " constr; ");
		System.out.print("obj = " + format.format(cplex.funcionObjetivo()) + "; ");
		System.out.print(cplex.variablesFraccionarias() + " fract");
		System.out.print(_solverCG.esDualViolado() ? " (DV)" : "");
		System.out.print(_solverCG.esPrimalViolado() ? " (PV)" : "");
		System.out.println();
	}

	private void crearVentana()
	{
    	_framePrincipal = new JFrame("Solucion primal");
        _framePrincipal.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        _framePrincipal.setSize(500, 500);
        
		_panelPrincipal = new DrawingPanel();
        _framePrincipal.add(_panelPrincipal);
	}
	
	private void crearDualizer()
	{
		_frameDualizer = new JFrame("Dualizador");
	    _frameDualizer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    _frameDualizer.setSize(500, 500);
	        
		_panelDualizer = new DrawingPanel();
		_frameDualizer.add(_panelDualizer);
	}

	private void mostrar(JFrame frame)
	{
		if( frame.isVisible() )
			frame.repaint();
		else
			frame.setVisible(true);
	}
	
	private void limpiar(DrawingPanel panel)
	{
		if( panel != null )
			panel.clear();
	}
}
