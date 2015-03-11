package heroesgrave.spade.core.effects;

import heroesgrave.spade.core.changes.MappingChange;
import heroesgrave.spade.editing.Effect;
import heroesgrave.spade.gui.dialogs.GridEffectDialog;
import heroesgrave.spade.gui.misc.WeblafWrapper;
import heroesgrave.spade.image.Layer;
import heroesgrave.utils.math.MathUtils;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.geom.Point2D.Float;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/*
 * TODO: generalize to any type of mapping
 * 
 * code is a bit wonky in places
 */
public class MappingEffect extends Effect {
	
	public MappingEffect(String name) {
		super(name);
	}
	
	@Override
	public void perform(final Layer layer) {
		final GridEffectDialog dialog = new GridEffectDialog(1, 1, "Mapping", getIcon());
		
		JPanel upperBottom = new JPanel(), lowerBottom = new JPanel();
		
		final JCheckBox[] boxes = {
				WeblafWrapper.createCheckBox(),
				WeblafWrapper.createCheckBox(),
				WeblafWrapper.createCheckBox() };
		JLabel[] labels = {
				WeblafWrapper.createLabel("Red"),
				WeblafWrapper.createLabel("Green"),
				WeblafWrapper.createLabel("Blue") };
		
		final MappingPanel mapping = new MappingPanel(new Consumer<MappingState>() {
			@Override
			public void accept(MappingState t) {
				if (t.mouse != null)
					dialog.getDialog().setTitle("(" + t.mouse.x + ", " + t.mouse.y + ")");
				else {
					dialog.getDialog().setTitle("Mapping");
					layer.getDocument().preview(new MappingChange(t.mapping));
				}
			}
		});
		
		for (int i = 0; i < boxes.length; i++) {
			final int idx = i;
			boxes[i].setSelected(true);
			
			boxes[i].addMouseListener(new MouseAdapter() {
				// Right click: solo-toggle
				// Middle click: all-on
				@Override
				public void mousePressed(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON3) {
						boolean q = false;
						for (int b = 0; b < boxes.length; b++)
							if (b != idx)
								q |= boxes[b].isSelected();
						for (int b = 0; b < boxes.length; b++)
							boxes[b].setSelected(b == idx || !q);
					}
					else if (e.getButton() == MouseEvent.BUTTON2)
						for (int b = 0; b < boxes.length; b++)
							boxes[b].setSelected(true);
				}
			});
			
			boxes[i].addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					mapping.enabled[idx] = boxes[idx].isSelected();
					mapping.repaint();
				}
			});
			
			upperBottom.add(boxes[i]);
			upperBottom.add(labels[i]);
		}
		
		JButton apply = WeblafWrapper.createButton("Apply");
		JButton cancel = WeblafWrapper.createButton("Cancel");
		
		apply.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.close();
				layer.getDocument().applyPreview();
			}
		});
		
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.close();
				layer.getDocument().preview(null);
			}
		});
		
		lowerBottom.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		lowerBottom.add(apply);
		lowerBottom.add(cancel);
		
		dialog.getBottomPanel().setLayout(new BorderLayout());
		dialog.getBottomPanel().add(upperBottom, BorderLayout.NORTH);
		dialog.getBottomPanel().add(lowerBottom, BorderLayout.SOUTH);
		
		dialog.getPanel(0).add(mapping);
		
		dialog.display();
		
		// hack I don't like
		dialog.getDialog().setLocation(dialog.getDialog().getX() - 500, dialog.getDialog().getY());
	}
	
	private static class MappingState {
		Point mouse;
		int[][] mapping;
		
		MappingState(Point m, int[][] mm) {
			mouse = m;
			mapping = mm;
		}
	}
	
	@SuppressWarnings("serial")
	private static class MappingPanel extends JComponent {
		
		PiecewiseLinearMapping[] mappings = {
				PiecewiseLinearMapping.identity(),
				PiecewiseLinearMapping.identity(),
				PiecewiseLinearMapping.identity() };
		
		int[][] lookups = new int[mappings.length][256];
		
		boolean[] enabled = { true, true, true };
		
		Map<Point2D.Float, Object> xFixed = new IdentityHashMap<>();
		List<Point2D.Float> active = new ArrayList<>();
		
		final int pradius = 4;
		
		final Comparator<Point2D.Float> xComp1 = new Comparator<Point2D.Float>() {
			@Override
			public int compare(Float o1, Float o2) {
				return java.lang.Float.compare(o1.x, o2.x);
			}
		};
		
		Comparator<GraphEdge> xComp2 = new Comparator<GraphEdge>() {
			@Override
			public int compare(GraphEdge o1, GraphEdge o2) {
				return java.lang.Float.compare(o1.a.x + o1.b.x, o2.a.x + o2.b.x);
			}
		};
		
		MappingPanel(final Consumer<MappingState> mouseConsumer) {
			setSize(400, 400);
			setPreferredSize(getSize());
			
			for (int i = 0; i < mappings.length; i++)
				for (Point2D.Float p : mappings[i].points)
					xFixed.put(p, null);
			
			MouseAdapter listener = new MouseAdapter() {
				Point2D.Float mouse = new Point2D.Float();
				
				@Override
				public void mousePressed(MouseEvent e) {
					Point p = e.getPoint();
					
					float tx = p.x / (float) getWidth();
					float ty = 1 - (p.y / (float) getHeight());
					
					if (mouseConsumer != null)
						mouseConsumer.accept(new MappingState(new Point(Math.round(tx * 255), Math.round(ty * 255)), null));
					
					if (e.getButton() == MouseEvent.BUTTON1) {
						if (active.isEmpty())
							for (int i = 0; i < mappings.length; i++)
								if (enabled[i]) {
									Point2D.Float t = new Point2D.Float(tx, ty);
									int idx = 0;
									while (mappings[i].points.get(idx).x < t.x)
										idx++;
									mappings[i].points.add(idx, t);
								}
					} else if (e.getButton() == MouseEvent.BUTTON3) {
						for (int i = active.size() - 1; i >= 0; i--) {
							Point2D.Float t = active.get(i);
							if (!xFixed.containsKey(t)) {
								for (int z = 0; z < mappings.length; z++)
									mappings[z].points.remove(t);
								active.remove(i);
							}
						}
					}
					
					setActive(p);
					repaint();
				}
				
				@Override
				public void mouseMoved(MouseEvent e) {
					setActive(e.getPoint());
					repaint();
				}
				
				@Override
				public void mouseDragged(MouseEvent e) {
					Point p = e.getPoint();
					p.x = MathUtils.clamp(p.x, 0, getWidth());
					p.y = MathUtils.clamp(p.y, 0, getHeight());
					float tx = p.x / (float) getWidth();
					float ty = 1 - (p.y / (float) getHeight());
					
					if (mouseConsumer != null)
						mouseConsumer.accept(new MappingState(new Point(Math.round(tx * 255), Math.round(ty * 255)), null));
					
					if (!active.isEmpty()) {
						for (Point2D.Float t : active)
							t.setLocation(xFixed.containsKey(t) ? t.x : tx, ty);
						for (int i = 0; i < mappings.length; i++)
							if (enabled[i])
								Collections.sort(mappings[i].points, xComp1);
						repaint();
					}
					
				}
				
				@Override
				public void mouseReleased(MouseEvent e) {
					fillLookups();
					if (mouseConsumer != null)
						mouseConsumer.accept(new MappingState(null, lookups));
					setActive(e.getPoint());
					repaint();
				}
				
				private void setActive(Point p) {
					mouse.x = p.x / (float) getWidth();
					mouse.y = 1 - (p.y / (float) getHeight());
					
					active.clear();
					for (int i = 0; i < mappings.length; i++)
						if (enabled[i])
							for (Point2D.Float pt : mappings[i].points)
								if (mouse.distance(pt) <= pradius * 1.7 / getWidth())
									active.add(pt);
				}
			};
			
			addMouseListener(listener);
			addMouseMotionListener(listener);
		}
		
		private void fillLookups() {
			float w = 1 / 255f;
			for (int c = 0; c < mappings.length; c++) {
				int[] lookup = lookups[c];
				for (int i = 0; i < lookup.length; i++)
					lookup[i] = (int) (mappings[c].get(i * w) * 255);
			}
		}
		
		@Override
		public void paint(Graphics gg) {
			
			Graphics2D g = (Graphics2D) gg;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			Pair<Set<GraphEdge>, Set<Pair<Point2D.Float, Color>>> graph = graph();
			
			Stroke tmp = g.getStroke();
			g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			
			GraphEdge[] edges = graph.a.toArray(new GraphEdge[graph.a.size()]);
			Arrays.sort(edges, xComp2); // stop z-fighting
			
			for (GraphEdge e : edges) {
				g.setColor(e.c);
				g.draw(new Line2D.Float(e.a.x * getWidth(), (1 - e.a.y) * getHeight(), e.b.x * getWidth(), (1 - e.b.y) * getHeight()));
			}
			
			for (Pair<Point2D.Float, Color> p : graph.b) {
				float radius = active.contains(p.a) ? pradius * 1.5f : pradius;
				g.setColor(p.b);
				g.draw(new Ellipse2D.Float(p.a.x * getWidth() - radius, (1 - p.a.y) * getHeight() - radius, 2 * radius, 2 * radius));
			}
			
			g.setStroke(tmp);
			
			g.setColor(Color.gray);
			g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		}
		
		private Pair<Set<GraphEdge>, Set<Pair<Point2D.Float, Color>>> graph() {
			Set<GraphEdge> edges = new HashSet<>();
			Set<Pair<Point2D.Float, Color>> points = new HashSet<>();
			boolean[] channels = new boolean[mappings.length];
			
			// get distinct edges and points from mappings
			for (int i = 0; i < mappings.length; i++) {
				if (enabled[i]) {
					PiecewiseLinearMapping mapping = mappings[i];
					Point2D.Float p = mapping.points.get(0);
					points.add(new Pair<>(p, (Color) null));
					for (int z = 1; z < mapping.points.size(); z++) {
						Point2D.Float t = mapping.points.get(z);
						edges.add(new GraphEdge(p, t));
						points.add(new Pair<>(t, (Color) null));
						p = t;
					}
				}
			}
			
			// color points
			for (Pair<Point2D.Float, Color> p : points) {
				for (int x = 0; x < channels.length; x++)
					channels[x] = enabled[x] && mappings[x].points.contains(p.a);
				p.b = blend(channels);
			}
			
			// color edges
			for (GraphEdge e : edges) {
				for (int i = 0; i < channels.length; i++) {
					int aidx = mappings[i].points.indexOf(e.a);
					int bidx = mappings[i].points.indexOf(e.b);
					channels[i] = enabled[i] && aidx != -1 && bidx != -1 && aidx == bidx - 1;
				}
				
				e.c = blend(channels);
			}
			
			return new Pair<>(edges, points);
		}
		
		private Color blend(boolean... channels) {
			int v = 255;
			for (int i = 0; i < channels.length; i++)
				if (channels[i])
					v -= 35;
			
			return new Color(channels[0] ? v : 0, channels[1] ? v : 0, channels[2] ? v : 0);
		}
		
		private static class GraphEdge {
			Point2D.Float a, b;
			Color c;
			
			GraphEdge(Point2D.Float x, Point2D.Float y) {
				a = x;
				b = y;
			}
			
			@Override
			public boolean equals(Object e) {
				if (e instanceof GraphEdge) {
					GraphEdge d = (GraphEdge) e;
					return (a.equals(d.a) && b.equals(d.b));
				} else
					return e.equals(this);
			}
		}
	}
	
	// not optimized
	private static class PiecewiseLinearMapping {
		
		List<Point2D.Float> points = new ArrayList<>();
		
		PiecewiseLinearMapping() {
			points.add(new Point2D.Float(0, 0));
			points.add(new Point2D.Float(1, 1));
		}
		
		float get(float x) {
			int i = 0;
			while (i < points.size() && points.get(i).x <= x)
				i++;
			
			Point2D.Float start = points.get(i - 1);
			Point2D.Float end = points.get(Math.min(i, points.size() - 1));
			
			return start.y + (end.y - start.y) * ((x - start.x) / (end.x - start.x));
		}
		
		static PiecewiseLinearMapping identity() {
			return new PiecewiseLinearMapping();
		}
	}
	
	// something something tuples
	private static class Pair<A, B> {
		A a;
		B b;
		
		Pair(A a, B b) {
			this.a = a;
			this.b = b;
		}
	}
	
	private static interface Consumer<T> {
		void accept(T t);
	}
}
