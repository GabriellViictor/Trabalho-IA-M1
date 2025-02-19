import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

import javax.swing.*;

import java.io.*;
import java.util.*;
import java.awt.image.*;
import javax.imageio.ImageIO;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class GamePanel extends JPanel implements Runnable {
	private static final int PWIDTH = 1000;
	private static final int PHEIGHT = 800;
	private Thread animator;
	private boolean running = false;
	private boolean gameOver = false;
	private ArrayList<GNodo> caminhoEncontrado = null;

	private BufferedImage dbImage;
	private Graphics2D dbg;


	int FPS, SFPS;
	int fpscount;

	Random rnd = new Random();

	BufferedImage imagemcharsets;
	BufferedImage fundo;

	boolean LEFT, RIGHT, UP, DOWN;

	int MouseX, MouseY;


	float sqX1 = 0;
	float sqY1 = 100;

	float sqX2 = 0;
	float sqY2 = 200;

	HashMap<Long, GNodo> mapaNodos = new HashMap<>();
	ArrayList<GPath> listaPaths = new ArrayList<>();

	double minLat = Double.MAX_VALUE;
	double minLon = Double.MAX_VALUE;
	double maxLat = Double.NEGATIVE_INFINITY;
	double maxLon = Double.NEGATIVE_INFINITY;

	double zoom = 0.02;
	double translateX = -39906.85500000017;
	double translateY = -6695.015000000035;

	GNodo selectedNodoA = null;
	GNodo selectedNodoB = null;

	public GamePanel() {

		setBackground(Color.white);
		setPreferredSize(new Dimension(PWIDTH, PHEIGHT));

		// create game components
		setFocusable(true);

		requestFocus(); // JPanel now receives key events

		if (dbImage == null) {
			dbImage = new BufferedImage(PWIDTH, PHEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
			dbg = (Graphics2D) dbImage.getGraphics();
		}


		// Adiciona um Key Listner
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();

				if (keyCode == KeyEvent.VK_LEFT) {
					LEFT = true;
					translateX += 20;
				}
				if (keyCode == KeyEvent.VK_RIGHT) {
					RIGHT = true;
					translateX -= 20;
				}
				if (keyCode == KeyEvent.VK_UP) {
					UP = true;
					translateY += 20;
				}
				if (keyCode == KeyEvent.VK_DOWN) {
					DOWN = true;
					translateY -= 20;
				}

				System.out.println(translateX + " " + translateY);
			}

			@Override
			public void keyReleased(KeyEvent e) {
				int keyCode = e.getKeyCode();

				if (keyCode == KeyEvent.VK_LEFT) {
					LEFT = false;
				}
				if (keyCode == KeyEvent.VK_RIGHT) {
					RIGHT = false;
				}
				if (keyCode == KeyEvent.VK_UP) {
					UP = false;
				}
				if (keyCode == KeyEvent.VK_DOWN) {
					DOWN = false;
				}
			}
		});

		addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent e) {
				// TODO Auto-generated method stub
				MouseX = e.getX();
				MouseY = e.getY();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				// TODO Auto-generated method stub
				MouseX = e.getX();
				MouseY = e.getY();
			}
		});

		addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent e) {
				int PW2 = PWIDTH / 2;
				int PH2 = PHEIGHT / 2;
				double lon = ((e.getX() - PW2) / zoom + PW2 - translateX + minLon * 100000) / 100000;
				double lat = ((e.getY() - PH2) / zoom + PH2 - translateY + minLat * 100000) / 100000;

				if (selectedNodoA == null) {
					System.out.println(lat + " / " + lon);
					selectedNodoA = nodoMaisProximo(lat, lon);
				} else if (selectedNodoB == null) {
					System.out.println(lat + " / " + lon);
					selectedNodoB = nodoMaisProximo(lat, lon);

					if (selectedNodoA != null && selectedNodoB != null) {
						caminhoEncontrado = executarAEstrela(selectedNodoA, selectedNodoB);
						if (caminhoEncontrado != null) {
							System.out.println("Caminho encontrado com sucesso!");
						} else {
							System.out.println("Nenhum caminho encontrado.");
						}
					}
				} else {
					System.out.println(lat + " / " + lon);
					selectedNodoA = nodoMaisProximo(lat, lon);
					selectedNodoB = null;
				}
			}



			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});

		addMouseWheelListener(e -> {
			//System.out.println("w "+e.getWheelRotation());
			if (e.getWheelRotation() > 0) {
				//double px = (translateX/zoom)+PWIDTH/2;
				//double py = (translateY/zoom)+PHEIGHT/2;

				zoom = zoom * 1.1f;

				//translateX = px*zoom-(PWIDTH/2)*zoom;
				//translateY = py*zoom-(PHEIGHT/2)*zoom;

			} else if (e.getWheelRotation() < 0) {
				//double px = (translateX/zoom)+PWIDTH/2;
				//double py = (translateY/zoom)+PHEIGHT/2;

				zoom = zoom * 0.9f;

				//translateX = px*zoom-(PWIDTH/2)*zoom;
				//translateY = py*zoom-(PHEIGHT/2)*zoom;
			}
		});


		try {
			fundo = ImageIO.read(Objects.requireNonNull(getClass().getResource("fundo.jpg")));
		} catch (IOException e) {
			System.out.println("Load Image error:");
		}

		MouseX = MouseY = 0;

		parseDocument();

	} // end of GamePanel()

	public void addNotify() {
		super.addNotify(); // creates the peer
		startGame(); // start the thread
	}

	private void startGame()
// initialise and start the thread
	{
		if (animator == null || !running) {
			animator = new Thread(this);
			animator.start();
		}
	} // end of startGame()

	public void stopGame()
// called by the user to stop execution
	{
		running = false;
	}


	public void run()
		/* Repeatedly update, render, sleep */ {
		running = true;

		long DifTime, TempoAnterior;

		int segundo = 0;
		DifTime = 0;
		TempoAnterior = System.currentTimeMillis();

		while (running) {

			gameUpdate(DifTime); // game state is updated
			gameRender(); // render to a buffer
			paintImmediately(0, 0, PWIDTH, PHEIGHT); // paint with the buffer

			try {
				Thread.sleep(1); // sleep a bit
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}

			DifTime = System.currentTimeMillis() - TempoAnterior;
			TempoAnterior = System.currentTimeMillis();

			if (segundo != ((int) (TempoAnterior / 1000))) {
				FPS = SFPS;
				SFPS = 1;
				segundo = ((int) (TempoAnterior / 1000));
			} else {
				SFPS++;
			}

		}
		System.exit(0); // so enclosing JFrame/JApplet exits
	} // end of run()

	public void parseDocument() {

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			//File file = new File("FloripaContinente.osm");
			File file = new File("itacorubi.osm");
			Document doc = builder.parse(file);

			NodeList nodeList = doc.getDocumentElement().getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);

				//System.out.println(""+node.getNodeName());
				if (node.getNodeName().equals("node")) {
					NamedNodeMap map = node.getAttributes();
					System.out.println("--> " + map.getNamedItem("id").getNodeValue());
					GNodo gnodo = new GNodo();
					gnodo.id = Long.parseLong(map.getNamedItem("id").getNodeValue());
					gnodo.lat = Double.parseDouble(map.getNamedItem("lat").getNodeValue());
					gnodo.lon = Double.parseDouble(map.getNamedItem("lon").getNodeValue());
					mapaNodos.put(gnodo.id, gnodo);
					if (gnodo.lat < minLat) {
						minLat = gnodo.lat;
					}
					if (gnodo.lon < minLon) {
						minLon = gnodo.lon;
					}

					if (gnodo.lon > maxLon) {
						maxLon = gnodo.lon;
					}

					if (gnodo.lat > maxLat) {
						maxLat = gnodo.lat;
					}
				}
				if (node.getNodeName().equals("way")) {
					GPath gpath = new GPath();
					NodeList subnodeList = node.getChildNodes();
					boolean descartar = true;
					for (int j = 0; j < subnodeList.getLength(); j++) {
						Node subnode = subnodeList.item(j);
						if (subnode.getNodeName().equals("nd")) {
							long ref = Long.parseLong(subnode.getAttributes().getNamedItem("ref").getNodeValue());
							gpath.idnodos.add(ref);
						}
						if (subnode.getNodeName().equals("tag")) {
							String sn = subnode.getAttributes().getNamedItem("k").getNodeValue();
							if (sn.equals("highway")) {
								descartar = false;
							}
						}
					}
					if (!descartar) {
						listaPaths.add(gpath);
						for (int z = 0; z < gpath.idnodos.size(); z++) {
							GNodo gnB = mapaNodos.get(gpath.idnodos.get(z));
							gnB.ativo = true;
						}
					}
				}
			}

			double cx = ((maxLon - minLon) / 2) * 100000;
			double cy = ((maxLat - minLat) / 2) * 100000;

			translateX = -cx;
			translateY = -cy;
			//translateX = ((double) PWIDTH / 2) - (cx * 100000 - minLon * 100000);
			//translateY = ((double) PHEIGHT / 2) - (cy * 100000 - minLat * 100000);

			//System.out.println(minLat + " " + maxLat);
			//System.out.println(minLon + " " + maxLon);
			//System.out.println(cx + " " + cy);

		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (Iterator<Long> iterator = mapaNodos.keySet().iterator(); iterator.hasNext(); ) {
			Long key = iterator.next();
			GNodo gnB = mapaNodos.get(key);
			if (!gnB.ativo) {
				iterator.remove();
			}
		}


		for (GPath gpath : listaPaths) {
			GNodo gnA = mapaNodos.get(gpath.idnodos.get(0));
			for (int j = 1; j < gpath.idnodos.size(); j++) {
				GNodo gnB = mapaNodos.get(gpath.idnodos.get(j));
				Aresta ar = new Aresta(gnA, gnB);
				gnA.listaArestas.add(ar);
				gnB.listaArestas.add(ar);
				//System.out.println("Aresta Size "+ar.size);

				gnA = gnB;
			}
		}

	}

	int timerfps = 0;

	private void gameUpdate(long DiffTime) {
		sqX1 += 0.5f;

		sqX2 = sqX2 + 100 * DiffTime / 1000.0f;
	}

	// draw the current frame to an image buffer
	private void gameRender()
	{
		dbg.setColor(Color.white);
		dbg.fillRect(0, 0, PWIDTH, PHEIGHT);
		dbg.setColor(Color.BLUE);
		dbg.drawString("FPS: " + FPS, 20, 20);

		AffineTransform trans = dbg.getTransform();


		dbg.translate(PWIDTH / 2, PHEIGHT / 2);
		dbg.scale(zoom, zoom);
		dbg.translate(-PWIDTH / 2, -PHEIGHT / 2);
		dbg.translate(translateX, translateY);

		for (GPath gpath : listaPaths) {
			GNodo gnA = mapaNodos.get(gpath.idnodos.get(0));
			for (int j = 1; j < gpath.idnodos.size(); j++) {
				GNodo gnB = mapaNodos.get(gpath.idnodos.get(j));
				int x1 = (int) (gnA.lon * 100000 - minLon * 100000);
				int y1 = (int) (gnA.lat * 100000 - minLat * 100000);
				int x2 = (int) (gnB.lon * 100000 - minLon * 100000);
				int y2 = (int) (gnB.lat * 100000 - minLat * 100000);
				gnA = gnB;
				dbg.drawLine(x1, y1, x2, y2);
			}
		}
		dbg.setColor(Color.red);
		for (Long key : mapaNodos.keySet()) {
			GNodo gnB = mapaNodos.get(key);
			int x = (int) (gnB.lon * 100000 - minLon * 100000);
			int y = (int) (gnB.lat * 100000 - minLat * 100000);
			if (gnB == selectedNodoA) {
				dbg.setColor(Color.green);
				dbg.fillRect(x - 20, y - 20, 40, 40);
			} else if (gnB == selectedNodoB) {
				dbg.setColor(Color.ORANGE);
				dbg.fillRect(x - 20, y - 20, 40, 40);
			} else {
				dbg.setColor(Color.red);
				dbg.fillRect(x - 1, y - 1, 3, 3);
			}
		}

		// Desenha o caminho encontrado
		if (caminhoEncontrado != null) {
			dbg.setColor(Color.GREEN); // Cor do caminho
			for (int i = 0; i < caminhoEncontrado.size() - 1; i++) {
				GNodo n1 = caminhoEncontrado.get(i);
				GNodo n2 = caminhoEncontrado.get(i + 1);
				int x1 = (int) (n1.lon * 100000 - minLon * 100000);
				int y1 = (int) (n1.lat * 100000 - minLat * 100000);
				int x2 = (int) (n2.lon * 100000 - minLon * 100000);
				int y2 = (int) (n2.lat * 100000 - minLat * 100000);
				dbg.drawLine(x1, y1, x2, y2);
			}
		}

		dbg.setTransform(trans);
	}


	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (dbImage != null)
			g.drawImage(dbImage, 0, 0, null);
	}

	public GNodo nodoMaisProximo(double lat, double lon) {
		GNodo nodoselecionado = null;
		double menorvalor = 100000;

		for (Long nodekey : mapaNodos.keySet()) {
			GNodo onodo = mapaNodos.get(nodekey);
			double dist = distNodo(onodo, lat, lon);
			if (nodoselecionado == null || dist < menorvalor) {
				menorvalor = dist;
				nodoselecionado = onodo;
			}
		}

		return nodoselecionado;
	}

	public static double distNodo(GNodo onodo, double lat, double lon) {
		double dlat = onodo.lat - lat;
		double dlon = onodo.lon - lon;
		return Math.sqrt(dlat * dlat + dlon * dlon);
	}

	public ArrayList<GNodo> executarAEstrela(GNodo inicio, GNodo fim) {
		PriorityQueue<GNodo> filaAberta = new PriorityQueue<>();
		HashSet<GNodo> filaFechada = new HashSet<>();
		HashMap<GNodo, GNodo> cameFrom = new HashMap<>();
		HashMap<GNodo, Double> gScore = new HashMap<>();
		HashMap<GNodo, Double> fScore = new HashMap<>();

		// Inicializar pontuações
		gScore.put(inicio, 0.0);
		fScore.put(inicio, inicio.distancia(fim));
		filaAberta.add(inicio);

		while (!filaAberta.isEmpty()) {
			GNodo atual = filaAberta.poll();

			if (atual.equals(fim)) {
				// Caminho encontrado
				return reconstruirCaminho(cameFrom, atual);
			}

			filaFechada.add(atual);

			for (Aresta aresta : atual.getArestas()) {
				GNodo vizinho = aresta.obterOutroNodo(atual);
				if (filaFechada.contains(vizinho)) continue;

				double tentativo_gScore = gScore.get(atual) + aresta.getCusto();

				if (!gScore.containsKey(vizinho) || tentativo_gScore < gScore.get(vizinho)) {
					cameFrom.put(vizinho, atual);
					gScore.put(vizinho, tentativo_gScore);
					fScore.put(vizinho, gScore.get(vizinho) + vizinho.distancia(fim));

					if (!filaAberta.contains(vizinho)) {
						filaAberta.add(vizinho);
					}
				}
			}
		}

		// Caso não encontre caminho
		return null;
	}


	public ArrayList<GNodo> reconstruirCaminho(HashMap<GNodo, GNodo> cameFrom, GNodo atual) {
		ArrayList<GNodo> caminho = new ArrayList<>();
		caminho.add(atual);

		while (cameFrom.containsKey(atual)) {
			atual = cameFrom.get(atual);
			caminho.add(0, atual); // Adiciona o nodo no início da lista
		}

		return caminho;
	}



	public static void main(String[] args) {
		GamePanel ttPanel = new GamePanel();

		// create a JFrame to hold the timer test JPanel
		JFrame app = new JFrame("Swing Timer Test");
		app.getContentPane().add(ttPanel, BorderLayout.CENTER);
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		app.pack();
		app.setResizable(false);
		app.setVisible(true);
	} // end of main()

} // end of GamePanel class
