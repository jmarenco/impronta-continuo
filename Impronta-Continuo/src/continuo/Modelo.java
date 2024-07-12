package continuo;

import java.util.Arrays;

import com.vividsolutions.jts.geom.Polygon;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import impronta.Instancia;
import impronta.Semilla;

public class Modelo
{
	private Instancia _instancia;
	private RepresentacionLineal _perimetro;
	private RepresentacionLineal[] _restriccion;
	
	private int _pads;
	private int _semillas;
	private int _restricciones;
	private int _maxRestriccion;
	private double M;
	
	private IloCplex _cplex;
	private IloNumVar[] x;
	private IloNumVar[] y;
	private IloNumVar[][] w;
	private IloNumVar[][] l;
	private IloNumVar[][] r;
	private IloNumVar[][] t;
	private IloNumVar[][] b;
	private IloNumVar[][][] s;
	
	private boolean _simetrias = false;
	private boolean _ascendentes = true;
	private Objetivo _objetivo = Objetivo.Cantidad;
	
	public enum Objetivo { Cantidad, Area };
	
	public Modelo(Instancia instancia, IloCplex cplex, int pads)
	{
		_instancia = instancia;
		_cplex = cplex;
		_pads = pads;
		_semillas = _instancia.getSemillas().size();
		_restricciones = instancia.getRestricciones().size();
		_restriccion = new RepresentacionLineal[_restricciones];
		_objetivo = Objetivo.Cantidad;
		
		if( instancia.getRegion().getGeometry() instanceof Polygon )
			_perimetro = new RepresentacionLineal(instancia.getRegion().getGeometry());
		
		for(int i=0; i<_restricciones; ++i)
			_restriccion[i] = new RepresentacionLineal(instancia.getRestricciones().get(i).getPolygon());
			
		M = Math.max(maxx()-minx(), maxy()-miny());
		_maxRestriccion = maxRestriccion();
	}
	
	public void setObjetivo(Objetivo objetivo)
	{
		_objetivo = objetivo;
	}
	public void setEliminacionSimetrias(boolean eliminacion)
	{
		_simetrias = !eliminacion;
		_ascendentes = eliminacion;
	}
	
	public void generar() throws IloException
	{
		generarVariables();
		generarObjetivo();
		generarRestricciones();
	}
	
	public void generarVariables() throws IloException
	{
		x = new IloNumVar[_pads];
		y = new IloNumVar[_pads];
		w = new IloNumVar[_pads][_semillas];
		l = new IloNumVar[_pads][_pads];
		r = new IloNumVar[_pads][_pads];
		t = new IloNumVar[_pads][_pads];
		b = new IloNumVar[_pads][_pads];
		s = new IloNumVar[_pads][_restricciones][_maxRestriccion];
		
		for(int i=0; i<_pads; ++i)
		{
			x[i] = _cplex.numVar(minx(), maxx(), "x" + i);
			y[i] = _cplex.numVar(miny(), maxy(), "y" + i);
		}
		
		for(int i=0; i<_pads; ++i)
		for(int k=0; k<_semillas; ++k)
		{
			w[i][k] = _cplex.boolVar("w" + i + "_" + k);
		}
			
		for(int i=0; i<_pads; ++i)
		for(int j=i+1; j<_pads; ++j)
		{
			l[i][j] = _cplex.boolVar("l" + i + "_" + j);
			t[i][j] = _cplex.boolVar("t" + i + "_" + j);
			
			if( _simetrias == true )
			{
				r[i][j] = _cplex.boolVar("r" + i + "_" + j);
				b[i][j] = _cplex.boolVar("b" + i + "_" + j);
			}
		}
		
		for(int i=0; i<_pads; ++i)
		for(int j=0; j<_restricciones; ++j)
		for(int k=0; k<_restriccion[j].restricciones(); ++k)
			s[i][j][k] = _cplex.boolVar("s" + i + "_" + j + "_" + k);
	}
	
	public void generarObjetivo() throws IloException
	{
		IloNumExpr obj = _cplex.linearIntExpr();
		
		for(int i=0; i<_pads; ++i)
		for(int k=0; k<_semillas; ++k)
			obj = _cplex.sum(obj, _cplex.prod(_objetivo == Objetivo.Area ? 1000 * semilla(k).getArea() : 1.0, w[i][k]));
		
		_cplex.addMaximize(obj);
	}
	
	public void generarRestricciones() throws IloException
	{
		// Pads dentro del perimetro
		for(int i=0; i<_pads; ++i)
		for(int j=0; j<_perimetro.restricciones(); ++j)
		{
			RepresentacionLineal.Restriccion restriccion = _perimetro.get(j);
			
			agregarInterno(restriccion, i, +1, +1);
			agregarInterno(restriccion, i, +1, -1);
			agregarInterno(restriccion, i, -1, +1);
			agregarInterno(restriccion, i, -1, -1);
		}

		// Liga las variables
		for(int i=0; i<_pads; ++i)
		for(int j=i+1; j<_pads; ++j)
		{
			agregarDefl(i, j);
			agregarDeft(i, j);
			
			if( _simetrias == true )
			{
				agregarDefr(i, j);
				agregarDefb(i, j);
			}
		}

		// Los pads no se superponen
		for(int i=0; i<_pads; ++i)
		for(int j=i+1; j<_pads; ++j)
			agregarSuperposicion(i, j);
		
		// Cada pad tiene a lo sumo una semilla
		for(int i=0; i<_pads; ++i)
			agregarSemillas(i);
		
		// No se usa un pad si no se uso el anterior
		for(int i=1; i<_pads && _ascendentes; ++i)
			agregarAscendente(i);
		
		// Definicion de las variables s
		for(int i=0; i<_pads; ++i)
		for(int j=0; j<_restricciones; ++j)
		for(int k=0; k<_restriccion[j].restricciones(); ++k)
			agregarDefs(i, j, k);
		
		// Se viola al menos una restriccion de cada restriccion
		for(int i=0; i<_pads; ++i)
		for(int j=0; j<_restricciones; ++j)
			agregarViolacion(i, j);
	}

	private void agregarInterno(RepresentacionLineal.Restriccion restriccion, int i, int sgx, int sgy) throws IloException
	{
		IloNumExpr lhs = _cplex.linearNumExpr(); 
		
		lhs = _cplex.sum(lhs, _cplex.prod(restriccion.ax, x[i]));
		lhs = _cplex.sum(lhs, _cplex.prod(restriccion.ay, y[i]));
		
		for(int k=0; k<_semillas; ++k)
			lhs = _cplex.sum(lhs, _cplex.prod(sgx * restriccion.ax * semilla(k).getLargo() / 2 + sgy * restriccion.ay * semilla(k).getAncho() / 2, w[i][k]));
		
		_cplex.addLe(lhs, restriccion.b);
	}
	
	private void agregarDefl(int i, int j) throws IloException
	{
		IloNumExpr lhs = _cplex.linearNumExpr();
		
		lhs = _cplex.sum(lhs, _cplex.prod(1, x[i]));
		lhs = _cplex.sum(lhs, _cplex.prod(-1, x[j]));
		lhs = _cplex.sum(lhs, _cplex.prod(M, l[i][j]));

		for(int k=0; k<_semillas; ++k)
		{
			lhs = _cplex.sum(lhs, _cplex.prod(semilla(k).getLargo()/2, w[i][k]));
			lhs = _cplex.sum(lhs, _cplex.prod(semilla(k).getLargo()/2, w[j][k]));
		}
		
		_cplex.addLe(lhs, M);
	}
	
	private void agregarDefr(int i, int j) throws IloException
	{
		IloNumExpr lhs = _cplex.linearNumExpr();
		
		lhs = _cplex.sum(lhs, _cplex.prod(-1, x[i]));
		lhs = _cplex.sum(lhs, _cplex.prod(1, x[j]));
		lhs = _cplex.sum(lhs, _cplex.prod(M, r[i][j]));

		for(int k=0; k<_semillas; ++k)
		{
			lhs = _cplex.sum(lhs, _cplex.prod(semilla(k).getLargo()/2, w[i][k]));
			lhs = _cplex.sum(lhs, _cplex.prod(semilla(k).getLargo()/2, w[j][k]));
		}
		
		_cplex.addLe(lhs, M);
	}
	
	private void agregarDeft(int i, int j) throws IloException
	{
		IloNumExpr lhs = _cplex.linearNumExpr();
		
		lhs = _cplex.sum(lhs, _cplex.prod(1, y[i]));
		lhs = _cplex.sum(lhs, _cplex.prod(-1, y[j]));
		lhs = _cplex.sum(lhs, _cplex.prod(M, t[i][j]));

		for(int k=0; k<_semillas; ++k)
		{
			lhs = _cplex.sum(lhs, _cplex.prod(semilla(k).getAncho()/2, w[i][k]));
			lhs = _cplex.sum(lhs, _cplex.prod(semilla(k).getAncho()/2, w[j][k]));
		}
		
		_cplex.addLe(lhs, M);
	}
	
	private void agregarDefb(int i, int j) throws IloException
	{
		IloNumExpr lhs = _cplex.linearNumExpr();
		
		lhs = _cplex.sum(lhs, _cplex.prod(-1, y[i]));
		lhs = _cplex.sum(lhs, _cplex.prod(1, y[j]));
		lhs = _cplex.sum(lhs, _cplex.prod(M, b[i][j]));

		for(int k=0; k<_semillas; ++k)
		{
			lhs = _cplex.sum(lhs, _cplex.prod(semilla(k).getAncho()/2, w[i][k]));
			lhs = _cplex.sum(lhs, _cplex.prod(semilla(k).getAncho()/2, w[j][k]));
		}
		
		_cplex.addLe(lhs, M);
	}

	private void agregarSuperposicion(int i, int j) throws IloException
	{
		IloNumExpr lhs = _cplex.linearNumExpr();
		
		lhs = _cplex.sum(lhs, l[i][j]);
		lhs = _cplex.sum(lhs, t[i][j]);
		
		if( _simetrias == true )
		{
			lhs = _cplex.sum(lhs, r[i][j]);
			lhs = _cplex.sum(lhs, b[i][j]);
		}

		for(int k=0; k<_semillas; ++k)
		{
			lhs = _cplex.sum(lhs, _cplex.prod(-1, w[i][k]));
			lhs = _cplex.sum(lhs, _cplex.prod(-1, w[j][k]));
		}

		_cplex.addGe(lhs, -1);
	}
	
	private void agregarSemillas(int i) throws IloException
	{
		IloNumExpr lhs = _cplex.linearNumExpr();

		for(int k=0; k<_semillas; ++k)
			lhs = _cplex.sum(lhs, w[i][k]);
		
		_cplex.addLe(lhs, 1);
	}
	
	private void agregarAscendente(int i) throws IloException
	{
		IloNumExpr lhs = _cplex.linearNumExpr();

		for(int k=0; k<_semillas; ++k)
		{
			lhs = _cplex.sum(lhs, _cplex.prod(1, w[i][k]));
			lhs = _cplex.sum(lhs, _cplex.prod(-1, w[i-1][k]));
		}
		
		_cplex.addLe(lhs, 0);
	}
	
	private void agregarDefs(int i, int j, int k) throws IloException
	{
		IloNumExpr lhs = _cplex.linearNumExpr();
		RepresentacionLineal.Restriccion restriccion = _restriccion[j].get(k);
		
		lhs = _cplex.sum(lhs, _cplex.prod(restriccion.ax, x[i]));
		lhs = _cplex.sum(lhs, _cplex.prod(restriccion.ay, y[i]));
		lhs = _cplex.sum(lhs, _cplex.prod(-100*M, s[i][j][k]));
		
		_cplex.addGe(lhs, restriccion.b - 100*M);
	}
	
	private void agregarViolacion(int i, int j) throws IloException
	{
		IloNumExpr lhs = _cplex.linearNumExpr();

		for(int k=0; k<_restriccion[j].restricciones(); ++k)
			lhs = _cplex.sum(lhs, s[i][j][k]);
		
		_cplex.addGe(lhs, 1);
	}

	public double maxx()
	{
		return _perimetro.maxx();
	}
	public double minx()
	{
		return _perimetro.minx();
	}
	public double maxy()
	{
		return _perimetro.maxy();
	}
	public double miny()
	{
		return _perimetro.miny();
	}
	public int maxRestriccion()
	{
		return Arrays.stream(_restriccion).mapToInt(r -> r.restricciones()).max().orElse(0);
	}
	
	private Semilla semilla(int k)
	{
		return _instancia.getSemillas().get(k);
	}
	
	public Instancia getInstancia()
	{
		return _instancia;
	}
	public IloCplex getCplex()
	{
		return _cplex;
	}
	
	public boolean permiteSimetrias()
	{
		return _simetrias;
	}
	public int getPads()
	{
		return _pads;
	}
	
	public IloNumVar getx(int i)
	{
		return x[i];
	}
	public IloNumVar gety(int i)
	{
		return y[i];
	}
	public IloNumVar getw(int i, int k)
	{
		return w[i][k];
	}
	public IloNumVar getl(int i, int j)
	{
		if( i >= j )
			throw new RuntimeException("Se consulto l(" + i + "," + j + ")");
		
		return l[i][j];
	}
	public IloNumVar getr(int i, int j)
	{
		if( i >= j )
			throw new RuntimeException("Se consulto r(" + i + "," + j + ")");
		
		return r[i][j];
	}
	public IloNumVar getb(int i, int j)
	{
		if( i >= j )
			throw new RuntimeException("Se consulto b(" + i + "," + j + ")");
		
		return b[i][j];
	}
	public IloNumVar gett(int i, int j)
	{
		if( i >= j )
			throw new RuntimeException("Se consulto t(" + i + "," + j + ")");
		
		return t[i][j];
	}
}
