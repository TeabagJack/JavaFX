package Visuals.main;

import Controller.*;
import Visuals.engine.graphics.Loader;
import Visuals.engine.graphics.MasterRenderer;
import Visuals.engine.graphics.models.TexturedModel;
import Visuals.engine.graphics.textures.ModelTexture;
import Visuals.engine.graphics.textures.TerrainTexture;
import Visuals.engine.graphics.textures.TerrainTexturePack;
import Visuals.engine.graphics.textures.objConverter.OBJFileLoader;
import Visuals.engine.io.Input;
import Visuals.engine.io.Window;
import Visuals.entities.Camera;
import Visuals.entities.Entity;
import Visuals.entities.Light;
import Visuals.entities.Player;
import Visuals.normalMappingObjConverter.NormalMappedObjLoader;
import Visuals.particles.ParticleBrain;
import Visuals.particles.particleGen;
import Visuals.particles.ParticleTexture;
import base.*;
import org.lwjgl.glfw.GLFW;
import org.lwjglx.util.vector.Vector2f;
import org.lwjglx.util.vector.Vector3f;
import org.lwjglx.util.vector.Vector4f;
import Visuals.terrain.Terrain;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public class Main implements Runnable {

	//game
	public Thread game;
	public Window window;
	public static final int WIDTH = 1600, HEIGHT = 900;
	public int i = 0;
	GameController g = new GameController();
	public static String testMapPath;
	public Loader loader = new Loader();
	public MasterRenderer renderer;
	public static Terrain terrain;
	public Camera camera;
	public Input input;
	public long lastClick;
	public int moveIndex = 0;
	public boolean mainMenuBoolean = false;

	//guard
	public TexturedModel texturedModelGuard;
	//intruder
	public TexturedModel texturedModelIntruder;
	//wall
	public TexturedModel texturedModelWall;
	//trace
	public TexturedModel texturedModelTrace;
	//goal
	public TexturedModel texturedModelgoal;
	//tower
	public TexturedModel texturedModelTower;
	// grass
	public TexturedModel texturedModelGrass;
	//portal Exit
	public TexturedModel texturedModelportalExit;
	//shade
	public TexturedModel texturedModelShade;

//	 Particle Generator

	public Visuals.particles.particleGen particleGen;

//	 blendMap for terrain and textures (texturePack, bgColorRGB)

	public TerrainTexture backgroundTexture;
	public TerrainTexture rTexture;
	public TerrainTexture gTexture;
	public TerrainTexture bTexture;
	public TerrainTexturePack texturePack;
	public TerrainTexture blendMap;

	private static final float RED = 0.5f;
	private static final float GREEN = 0.5f;
	private static final float BLUE = 0.5f;
	public static int L = (int) (Terrain.SIZE/2);

	public Player guard;
	public Player intruder;
	public Player camPlayer;

	public List<Light> lights;
	public List<Entity> entities = new ArrayList<>();
	public List<Entity> transparentEntities = new ArrayList<>();
	public List<Player> players = new ArrayList<>();
	public List<Player> guards = new ArrayList<>();
	public List<Player> intruders = new ArrayList<>();
	public List<Entity> normalMappedEntities = new ArrayList<>();
	public List<Entity> listOfTraceEntities = new ArrayList<>();
	public List<Entity> entitiesTimeStep1 = new ArrayList<>();

	public ArrayList<ArrayList<int[]>> listOfMovesAllGuard;
	public ArrayList<ArrayList<int[]>> listOfMovesAllIntruder;
	public ArrayList<Entity> walls;

	int escapeCount = 0;
	boolean maxMoveSizeFound = false;
	int maxSize = 0;
	public static boolean guardsAdded;


	/**
	 * Start instance of OpenGL Graphics Engine
	 */
	public void start() {
		game = new Thread(this, "Simulation");
		game.start();
	}

	/**
	 * Initialize the objects, at the start of the game, just once.
	 * @throws Exception
	 */

	public void init() throws Exception {
		window = new Window(WIDTH, HEIGHT, "Simulation");
		window.setBackgroundColor(RED, GREEN, BLUE);
		window.create();
		renderer = new MasterRenderer(loader, camera);
		ParticleBrain.init(loader,renderer.getProjectionMatrix());
		input = new Input();


		/*
		Starting of the GameController (back end engine)
		 */

		g = new GameController();
		g.startGame();



		//parser.readFile(testMapPath)


		// create texture for terrain
		backgroundTexture = new TerrainTexture(loader.loadTexture("beach"));
		rTexture = new TerrainTexture(loader.loadTexture("dirt"));
		gTexture = new TerrainTexture(loader.loadTexture("pinkFlowers"));
		bTexture = new TerrainTexture(loader.loadTexture("path"));
		texturePack = new TerrainTexturePack(backgroundTexture, rTexture, gTexture, bTexture);
		blendMap = new TerrainTexture(loader.loadTexture("blendMap"));

		// * step 3: Add textures and models together.

		// Loading in an object:

		texturedModelGuard = new TexturedModel(OBJFileLoader.loadOBJ("untitled", loader), new ModelTexture(loader.loadTexture("floatrobot")));
		texturedModelIntruder = new TexturedModel(OBJFileLoader.loadOBJ("mira", loader), new ModelTexture(loader.loadTexture("miraTexture")));
		texturedModelgoal = new TexturedModel(OBJFileLoader.loadOBJ("chest", loader), new ModelTexture(loader.loadTexture("chest")));
		texturedModelWall = new TexturedModel(OBJFileLoader.loadOBJ("wallv2", loader), new ModelTexture(loader.loadTextureTransparent("wallv2Texture")));
		texturedModelTower = new TexturedModel(OBJFileLoader.loadOBJ("remove", loader), new ModelTexture(loader.loadTexture("chest")));
		texturedModelGrass = new TexturedModel(OBJFileLoader.loadOBJ("tallgrass", loader), new ModelTexture(loader.loadTexture("green_alpha_tall_grass")));
		texturedModelportalExit = new TexturedModel(OBJFileLoader.loadOBJ("portalExit1", loader), new ModelTexture(loader.loadTexture("portalBase")));
		texturedModelShade = new TexturedModel(OBJFileLoader.loadOBJ("shade", loader), new ModelTexture(loader.loadTexture("beachShade")));

		TexturedModel teleportNormalModel = new TexturedModel(NormalMappedObjLoader.loadOBJ("teleExit", loader), new ModelTexture(loader.loadTexture("portalcolor")));
		teleportNormalModel.getTexture().setNormalMap(loader.loadTexture("portalNormal"));

		texturedModelportalExit = new TexturedModel(NormalMappedObjLoader.loadOBJ("portalExit1", loader), new ModelTexture(loader.loadTexture("portalBase")));
		texturedModelportalExit.getTexture().setNormalMap(loader.loadTexture("portalBaseNormal"));

		//trace
		texturedModelTrace = new TexturedModel(OBJFileLoader.loadOBJ("boots12", loader), new ModelTexture(loader.loadTexture("chest")));

		terrain = new Terrain(0, 0, loader, texturePack, blendMap, "heightMap", "grassnormal");
		walls = createWallsFromFile();
		generateBorderWalls();
		createShadesFromFile();


		// teleporter
		for (Teleport t : GameController.variables.getPortals()) {
			ArrayList<int[]> entrances = t.getPointsIn();
			int[] destination = t.getPointOut();
			float telePosX = (t.getCoordIn().get(0) + t.getCoordIn().get(2)) / 2;
			float telePosY = (t.getCoordIn().get(1) + t.getCoordIn().get(3)) / 2;
			double angle = t.getDegreeOut();
			normalMappedEntities.add(new Entity(teleportNormalModel, new Vector3f(telePosX + L, terrain.getHeightOfTerrain(telePosX+L, telePosY+L), telePosY + L), (float) 0, (float) angle, (float) 0, 3, 1));
			entities.add(new Entity(texturedModelportalExit, new Vector3f(destination[0] + L, terrain.getHeightOfTerrain(telePosX+L, telePosY+L), destination[1] + L), (float) 0, (float) angle, 0, 1, 1));
		}

		// grass
		for (int j = 0; j < 500; j++) {
			int randomX = ThreadLocalRandom.current().nextInt(0, GameController.variables.getWidth() + 1);
			int randomY = ThreadLocalRandom.current().nextInt(0, GameController.variables.getWidth() + 1);
			int randomRotation = ThreadLocalRandom.current().nextInt(0, 360 + 1);
			entities.add(new Entity(texturedModelGrass, new Vector3f(L + randomX, terrain.getHeightOfTerrain(L + randomX, L + randomY), randomY + L), 0, randomRotation, 0, 1, 1));
		}

		// goal
		for (Tile t : GameController.goalTiles) {
			entities.add(new Entity(texturedModelgoal, new Vector3f(t.getXCoord() + L, terrain.getHeightOfTerrain(t.getXCoord()+L, t.getYCoord()+L), t.getYCoord() + L), (float) 0, 90, 0, 1, 1));
		}

		// generate light
		lights = new ArrayList<>();
		lights.add(new Light(new Vector3f(1000000 + L, 1000000, 300000 + L), new Vector3f(1f, 1f, 1f)));

		// generate players
		// * step 4: Generate entities or players.

		for (int j = 0; j < GameController.variables.getNumberOfIntruders(); j++) {
			int[] pathIntruder = GameController.intruderSpawnPoints.get(j);

			intruders.add(new Player(texturedModelIntruder, new Vector3f(pathIntruder[0] + L, terrain.getHeightOfTerrain(pathIntruder[0] + L, pathIntruder[1] + L), pathIntruder[1] + L), 0, 90, 0, 0.5f, j));
		}

		for (int j = 0; j < GameController.variables.getNumberOfGuards(); j++) {
				guards.add(new Player(texturedModelGuard, new Vector3f(0 + L, terrain.getHeightOfTerrain(0 + L, 0 + L), 0 + L), 0, 90, 0, 1, j));
		}

		camPlayer = new Player(texturedModelIntruder, new Vector3f(L + 50, 0, L + 50), 0, 90, 0, 1, 1);

		entities.addAll(walls);

		// put the camera
		camera = new Camera(intruders.get(0));

		lastClick = System.currentTimeMillis();

		// particleSystem
		ParticleTexture particleTexture = new ParticleTexture(loader.loadTexture("explosion02"), 5);
		particleGen = new particleGen(particleTexture,1,5,0.3f,1,20);
		particleGen.randomizeRotation();
		particleGen.setDirection(new Vector3f(0,1,0),0f);
		particleGen.setLifee(0.1f);
		particleGen.setSpeede(0.4f);
		particleGen.setScalee(0f);

	}

	public void run() {
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (!window.shouldClose()) {
			update();
			//renderer.renderShadowMap(entities, lights.get(0));
			render();


			if (Input.isKeyDown(GLFW.GLFW_KEY_F11)) window.setFullscreen(!window.isFullscreen());

			if (Input.isKeyDown(GLFW.GLFW_KEY_ESCAPE)&& escapeCount<1) {
					escapeCount++;
					g = new GameController();
					g.startGame();

					// no guards
					g.makeAgentsLearn();

					// with guards
					g.addGuards();
					guardsAdded = true;
					g.runRaycast();
					g.makeAgentsMoveSmartly();
			}

			if(Input.isKeyDown(GLFW.GLFW_KEY_E)&& !maxMoveSizeFound&& escapeCount!=0){
				for (int j = 0; j < GameController.pathOfAllGuards.size(); j++) {
					if(GameController.pathOfAllGuards.get(j).size()>maxSize){
						maxSize = GameController.pathOfAllGuards.get(j).size();
					}
				}
				maxMoveSizeFound = true;
			}

			if (Input.isKeyDown(GLFW.GLFW_KEY_E) && moveIndex < maxSize && escapeCount!=0) {
				long currTime = System.currentTimeMillis();
				if (currTime - lastClick > 25) {
					// agents
					int count = 0;
					int index = 0;

					ArrayList<int[]> trace = GameController.listOfTrace.get(moveIndex);
					for (int[] traceSingleAgent: trace) {
						for (int j = 0; j < traceSingleAgent.length; j++) {
							if(count>=2) {
								listOfTraceEntities.remove(0);
							}
							listOfTraceEntities.add(new Entity(texturedModelTrace, new Vector3f(traceSingleAgent[0]+L,0, traceSingleAgent[1]+L),0,0,0,1,1));
						}
						count++;
					}

					for (int i = 0; i < GameController.pathOfAllIntruders.size(); i++) {
						if (i < GameController.variables.getNumberOfGuards()) {
							// path of each Agent
							ArrayList<int[]> pathIntruder = GameController.pathOfAllIntruders.get(i);

							if (pathIntruder.size() > moveIndex) {
//								{newX, newY, angle, seesTrace, SeesIntruder, hearsYell}
								for (int j = 0; j < pathIntruder.size(); j++) {
									intruders.get(i).move(new Vector2f(pathIntruder.get(moveIndex)[0] + L, pathIntruder.get(moveIndex)[1] + L ), pathIntruder.get(moveIndex)[2]);
								}
							}

							else if (moveIndex > pathIntruder.size()) {
								Vector3f position = intruders.get(i).getPosition();
								for (int j = 0; j < 100; j++) {
									particleGen.generateParticles(position);
								}
								intruders.get(i).move(new Vector2f(-500, -500), 0);
							}
							lastClick = currTime;
						}

						// same for guards.
						if (!guards.isEmpty()) {
							if (i < GameController.agents.size()) {
								// path of each guard
								ArrayList<int[]> pathGuard = GameController.pathOfAllGuards.get(i);
								if (pathGuard.size() > moveIndex) {
									for (int j = 0; j < pathGuard.size(); j++) {
										guards.get(i).move(new Vector2f(pathGuard.get(moveIndex)[0] + L, pathGuard.get(moveIndex)[1] + L), pathGuard.get(moveIndex)[2]);
									}
								}
								lastClick = currTime;
							}
						}
					}
					moveIndex++;
				}
			}
		}
		ParticleBrain.cleanUp();
		renderer.cleanUp();
		loader.cleanUp();
		window.destroy();
	}


	private void update() {
		window.update();
	}

	private void render() {
		camera.move();
		ParticleBrain.update(camera);
		Vector3f particlePos = new Vector3f(intruders.get(0).getPosition().x,8,intruders.get(0).getPosition().z);

		if (Input.isKeyDown(GLFW.GLFW_KEY_R)) {
			for( Entity entity : listOfTraceEntities){
				renderer.processEntity(entity);
			}
		}

	// * step 5: renderer.processEntity(nameOfEntity that you made at step 4.)
		for (Player pieces : guards) {
			renderer.processEntity(pieces);
		}
		for (Player pieces : intruders) {
			renderer.processEntity(pieces);
		}
		for (Entity entity : entities) {
			renderer.processEntity(entity);
		}
		for (Entity entity : normalMappedEntities) {
			renderer.processNormalMapEntity(entity);
		}
		for (Entity entity : transparentEntities){
			renderer.processEntity(entity);
		}

		renderer.processTerrain(terrain);
		renderer.render(lights, camera, new Vector4f(0, -1, 0, 100));
		ParticleBrain.renderParticles(camera);
		window.swapBuffers();
	}

	private void createShadesFromFile() {
		for (Shade shade : GameController.variables.getShades()) {
			if(shade.x1==shade.x3){
				for(int[] tile : shade.getPoints()){
					transparentEntities.add(new Entity(texturedModelShade, new Vector3f(tile[0]+L,0,tile[1]+L),0,0,0,1,1));
				}
			}
			else{
				for(int[] tile : shade.getPoints()){
					transparentEntities.add(new Entity(texturedModelShade, new Vector3f(tile[0]+L,0,tile[1]+L),0,90,0,1,1));
				}
			}
		}
	}

	private ArrayList<Entity> createWallsFromFile() {
		ArrayList<Entity> walls = new ArrayList<>();

		for (Wall w : GameController.variables.getWalls()) {
			if(w.x1==w.x3){
				for(int[] tile : w.getPoints()){
					walls.add(new Entity(texturedModelWall, new Vector3f(tile[0]+L,0,tile[1]+L),0,0,0,1,1));
				}
			}
			else{
				for(int[] tile : w.getPoints()){
					walls.add(new Entity(texturedModelWall, new Vector3f(tile[0]+L,0,tile[1]+L),0,90,0,1,1));
				}
			}
		}
		return walls;
	}

	private void generateBorderWalls(){
		for (int x = -1; x < GameController.variables.getHeight(); x++) {
			walls.add(new Entity(texturedModelWall, new Vector3f(x + L, terrain.getHeightOfTerrain(x+L, -1+L), -1 + L), 0, 90, 0, 1, 1));
		}
		for (int x = -1; x < GameController.variables.getHeight(); x++) {
			walls.add(new Entity(texturedModelWall, new Vector3f(x + L, terrain.getHeightOfTerrain(x+L, GameController.variables.getWidth()+L), GameController.variables.getWidth() + L), 0, 90, 0, 1, 1));
		}
		for (int y = -1; y < GameController.variables.getWidth(); y++) {
			walls.add(new Entity(texturedModelWall, new Vector3f(-1 + L, 0, y + L), 0, 0, 0, 1, 1));
		}
		for (int y = -1; y < GameController.variables.getWidth(); y++) {
			walls.add(new Entity(texturedModelWall, new Vector3f(GameController.variables.getHeight() + L, 0, y + L), 0, 0, 0, 1, 1));
		}
	}

}
