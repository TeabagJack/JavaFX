package QLearning;

import Controller.Map;
import Controller.Teleport;
import Controller.Tile;
import base.*;
import com.example.javafx.HelloController;

import java.util.ArrayList;

import static base.GameController.*;
import static base.Yell.YELL_RADIUS;

public class QLearning {

    public static double
            LEARNING_RATE = 0.3,
            DISCOUNT_FACTOR = 0.7,
            RANDOMNESS_LEVEL = 0.1;
    public static int
            LEARNING_CYCLES = 1000,
            MOVE_LIMIT = 1000;
    public static byte
            NUMBER_OF_POSSIBLE_ACTIONS = 4;
    public static byte
            MOVE_UP = 0,
            MOVE_RIGHT = 1,
            MOVE_DOWN = 2,
            MOVE_LEFT = 3;

    public static int VISION_RANGE = 0;

    public final Map map;
    public LearnerAgent agent;
    public QTable qTable;
    public RewardTable rewardTable;
    public EMTable emTable;
    public boolean gameEnded;

    /**
     *
     * check the static reward table and sum it with occurrences and
     * thus reward values from it's sensors.
     *
     */

    public QLearning(LearnerAgent agent) {
        if(!HelloController.readVariables){
            LEARNING_RATE = 0.3;
            DISCOUNT_FACTOR = 0.7;
            RANDOMNESS_LEVEL = 0.1;
            LEARNING_CYCLES = 1;
            MOVE_LIMIT = 1000;
        }
        else{
            LEARNING_RATE = HelloController.lr;
            DISCOUNT_FACTOR = HelloController.df;
            RANDOMNESS_LEVEL = HelloController.r;
            LEARNING_CYCLES = (int) HelloController.cc;
            MOVE_LIMIT = (int) HelloController.ml;
        }
        System.out.println(LEARNING_CYCLES);

        this.map = GameController.map;
        this.agent = agent;
        this.qTable = new QTable(map);
        rewardTable = new RewardTable(agent);
        emTable = new EMTable(agent.getPosition());
    }
    
    public static ArrayList<Integer> moveCounts = new ArrayList<>();

    public void learn(){
        VISION_RANGE = 0;

        // initialize all QTables
        for(Agent a : agents){
            // change vision range to make it learn faster
            a.setVisionRange(VISION_RANGE);

            if(a.getClass().getSuperclass() == LearnerAgent.class){
                ((LearnerAgent)a).getBrain().getQTable().initialize();
            }
        }

        int moveCount = -5;

        for (int cycleCount = 0; cycleCount < LEARNING_CYCLES; cycleCount++) {
            moveCount = 0;
            // while round has not ended yet
            while(!GameEndChecker.isInTerminalState()){
                for (int i = 0; i < agents.size(); i++){
                    Agent a = agents.get(i);
                    if (a.getClass() == Intruder.class) {
                        this.agent = (Intruder) a;
                        this.agent.getBrain().getEmTable().updateEMtable(moveCount,this.agent.getPosition());
                        byte action = getNextAction();
                        tryPerformingAction(action);
                        updateQValue();
                    }
                }
                moveCount++;
            }
            moveCounts.add(moveCount);
            System.out.println( (cycleCount+1) + "," + moveCount );
            putAgentsBackOnSpawn();
        }

        VISION_RANGE = variables.getVisionRange();
    }

    public void moveSmartly() {
        gameEnded = false;

        int moveCount = 0;

        for(Agent a : agents) {
            a.setVisionRange(variables.getVisionRange());
        }

        // while round has not ended yet
        while (!GameEndChecker.isInTerminalState()) {

            ArrayList<int[]> traceList = new ArrayList<>();

            // for every agent
            for (int i = 0; i < agents.size(); i++) {
                Agent a = agents.get(i);

                // if it's an intruder
                if (a.getClass() == Intruder.class) {

                    // move agent
                    this.agent = (Intruder) a;
                    this.agent.getBrain().getEmTable().updateEMtable(moveCount, this.agent.getPosition());
                    byte action = getNextAction();
                    tryPerformingAction(action);
                    updateQValue();
                    // save for gui
                    int newX = this.agent.getX(), newY = this.agent.getY(), index = Intruders.indexOf(a);
                    pathOfAllIntruders.get(index).add(makeIntruderData(newX, newY, (Intruder) agent));
                    System.out.println(this.agent.trace.getTraceTiles().size());
                    for (int j = 0; j < this.agent.trace.getTraceTiles().size(); j++) {
                        traceList.add(this.agent.trace.getTraceTiles().get(j));
                    }

                }
                // if it's a guard
                else if (a.getClass() == Guard.class) {

                    // move guard
                    ((Guard) a).makeMove();

                    // save for gui
                    int newX = a.getX(), newY = a.getY(), index = Guards.indexOf(a);
                    pathOfAllGuards.get(index).add(makeGuardData(newX, newY, (Guard) a));
                    for (int j = 0; j < this.agent.trace.getTraceTiles().size(); j++) {
                        traceList.add(a.trace.getTraceTiles().get(j));
                    }
                }
            }
            // increment move count
            moveCount++;

            listOfTrace.add(traceList);
        }
        gameEnded = true;
        putAgentsBackOnSpawn();
    }

    /**
     * performs random action x% of the time, and performs action with highest Q all other times
     * */
    private byte getNextAction(){
        double rand = Math.random();
        if(rand <= RANDOMNESS_LEVEL) {
            return getRandomAction();
        }
        else{
            return getActionWithHighestQ();
        }
    }

    private byte getActionWithHighestQ(){
        int[] currentState = this.agent.getPosition();
        return this.agent.getBrain().getQTable().getActionWithHighestQAtState(currentState);
    }

    public static byte getRandomAction(){
        byte min = 0, max = (byte) (NUMBER_OF_POSSIBLE_ACTIONS-1);
        return (byte) (Math.random() * (max - min) + min);
    }

    /**
     * updates q-value at previous state after action is performed
     * */
    private void updateQValue(){
        int[] previousState = this.agent.getPreviousState();
        byte actionPerformed = this.agent.getActionPerformed();
        double oldQ = this.agent.getBrain().getQTable().getQ(previousState,actionPerformed);
        double TD = temporalDifference();
        double newQ = oldQ + LEARNING_RATE * TD; // as per the Bellman Equation
        this.agent.getBrain().getQTable().setQ(previousState, actionPerformed, newQ);
    }

    /** returns how much the q-value changes at previous state after action is performed
     */
    private double temporalDifference(){
        int[] currentState = this.agent.getPosition();
        int[] previousState = this.agent.getPreviousState();
        byte actionPerformed = this.agent.getActionPerformed();

        double reward = rewardTable.getReward(currentState[0],currentState[1]);
        double maxQ = this.agent.getBrain().getQTable().getHighestQAvailableAtPosition(currentState[0], currentState[1]);
        double oldQ = this.agent.getBrain().getQTable().getQ(previousState,actionPerformed);
        return (reward + DISCOUNT_FACTOR * maxQ - oldQ);
    }

    private void tryPerformingAction(byte action){
        try {
            performAction(action);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void performAction(byte action) throws Exception {
        int[] currentState = this.agent.getPosition();

        int[] newPosition = {,};

        ArrayList[] visionScanResults = visionScan();
        ArrayList
                guardsSeen = visionScanResults[0],
                tracesSeen = visionScanResults[1];
        boolean
                seesGuard = guardsSeen.size() > 0,
                seesTrace = tracesSeen.size() > 0;

        // set booleans
        if(agent instanceof Intruder){
            ((Intruder) agent).setSeesGuard(seesGuard);
            ((Intruder) agent).setSeesTrace(seesTrace);
        }

        // if they see guard
        if(seesGuard){
            newPosition = runAway(guardsSeen);
            action = getActionFromNewPosition(newPosition);
        }

        // if they see trace
        else if(seesTrace){
            newPosition = runAway(tracesSeen);
            action= getActionFromNewPosition(newPosition);
        }

        // if they hear yell
        else if (agent.hearsYell()){

            // if he still hears yell, go to furthest neighbour from yell source
            int[] yellSource = agent.getAudioObject().getPosition();
            double distanceFromYellSource = RewardTable.distanceBetweenPoints(agent.getX(), agent.getY(), yellSource[0], yellSource[1]);
            if(distanceFromYellSource < YELL_RADIUS){
                int[][] neighbours =  agent.getAllNeighbours();

                double highestDistanceFromYell = Double.MIN_VALUE;
                int[] furthestNeighbour = {,};

                for(int[] neighbour : neighbours){
                    double distance = RewardTable.distanceBetweenPoints(neighbour[0], neighbour[1], yellSource[0], yellSource[1]);
                    if(distance > highestDistanceFromYell){
                        highestDistanceFromYell = distance;
                        furthestNeighbour = neighbour;
                    }
                }

                newPosition = furthestNeighbour;
                action = getActionFromNewPosition(newPosition);
            }
        }


        else
            newPosition = getValidPositionFromAction(action);

        agent.setActionPerformed(action);

        // check teleport
        for(Teleport tp : variables.getPortals()){
            for(int[] p : tp.getPointsIn()){
                if( p[0] == newPosition[0]  &&  p[1] == newPosition[1]){
                    agent.getTrace().addToTrace(new int[]{agent.getX(), agent.getY()});
                    newPosition = tp.getPointOut();
                    agent.setAngleDeg(tp.getDegreeOut());
                }
            }
        }

        // check if is in shaded area
        if(map.getTile(newPosition).hasShade()){
            agent.setVisionRange(VISION_RANGE/10);
        }
        else agent.setVisionRange(VISION_RANGE);

        // update learning state
        int[] newState = new int[]{newPosition[0], newPosition[1]};
        agent.setPreviousState(new int[]{currentState[0],currentState[1]});

        // update stress
        agent.getTrace().addToTrace(new int[]{agent.getX(), agent.getY()});

        // set position
        agent.setPosition(newState[0], newState[1]);
        agent.getSavedPath().add(new int[]{newPosition[0], newPosition[1]});

        // vision update
        agent.visionT.clear();
        agent.getRayEngine().calculate(agent);
        agent.visionT = agent.getRayEngine().getVisibleTiles(agent);
    }


    private void putAgentsBackOnSpawn(){
        ArrayList<Agent> newAgents = new ArrayList<>();

        for(Agent i : intrudersCaught) {
            i.putBackOnSpawn();
            agents.add(i);
            newAgents.add(i);
        }

        for(Agent a : agents) {
            a.putBackOnSpawn();
            newAgents.add(a);
        }

        agents = newAgents;
        intrudersCaught.clear();

    }

    private boolean newPositionIsValid(int newX, int newY) {
        return Map.inMap(newX, newY) && !map.hasWall(newX, newY);
    }

    private int[] getValidPositionFromAction(byte action) throws Exception {
        int[] currentState = this.agent.getPosition();
        int[] newPositionData = getNewPositionFromAction(action, currentState);

        if(!newPositionIsValid(newPositionData[0], newPositionData[1])){
            if(action == NUMBER_OF_POSSIBLE_ACTIONS-1){
                agent.setActionPerformed(action);
                return getValidPositionFromAction((byte) 0);
            }
            else {
                action = getRandomAction();
                agent.setActionPerformed(action);
                return getValidPositionFromAction(action);
            }
        }
        // set angle
        agent.setAngleDeg(newPositionData[2]);
        return new int[]{newPositionData[0], newPositionData[1], (int) agent.getAngleDeg()};
    }

    public QTable getQTable() {
        return qTable;
    }

    public static int[] getNewPositionFromAction(byte action, int[] currentState) throws Exception {
        int angle;
        int newX = currentState[0], newY = currentState[1];
        if(action == MOVE_UP) {
            newX -= 1;
            angle = 180;
        }
        else if (action == MOVE_RIGHT){
            newY += 1;
            angle = 90;
        }
        else if (action == MOVE_DOWN){
            newX += 1;
            angle = 0;
        }
        else if (action == MOVE_LEFT) {
            newY -= 1;
            angle = 270;
        }
        else {
            System.out.println("action number " + action);
            throw new Exception("action number not recognized");
        }
        return new int[]{newX, newY, angle};
    }

    public byte getActionFromNewPosition(int[] newPosition){
        int x = agent.getX(), y = agent.getY();
        int newX = newPosition[0], newY = newPosition[1];

        if(x != newX){
            if(newX == x+1)
                return MOVE_DOWN;
            else return MOVE_UP;
        }
        else {
            if(newY == y+1)
                return MOVE_RIGHT;
            else return MOVE_LEFT;
        }
    }

    /** this method scans the agents vision for guards and traces
     * @return array of lists { guardsSeen, tracesSeen }
     */
    public ArrayList<int[]>[] visionScan() {
        ArrayList<int[]>
                guardsSeen = new ArrayList<>(),
                tracesSeen = new ArrayList<>();

        // update vision
        agent.visionT = agent.getRayEngine().getVisibleTiles(agent);

        // for every tile in vision range
        for (int[] tilePos : agent.visionT) {

            // check for guards
            for (Agent guard : agents) {
                if (guard.getClass() == Guard.class) {
                    if (guard.getX() == tilePos[0] && guard.getY() == tilePos[1]) {
                        guardsSeen.add(new int[]{guard.getX(), guard.getY()});
                    }
                }
            }

            // check for trace: react only to guard's trace or stressed intruders
            Tile tile = map.getTile(tilePos);
            if (tile.hasTrace()) {
                Trace trace = tile.getTrace();
                boolean
                        traceOwnerIsGuard = trace.getOwner().getClass() == Guard.class,
                        traceOwnerIsStressed = trace.getStressLevel() > 0,
                        isNotOwnTrace = trace.getOwner() != this.agent;

                if ((traceOwnerIsGuard || traceOwnerIsStressed) && isNotOwnTrace) {
                    tracesSeen.add(tile.getPosition());
                }
            }
        }

        ArrayList<int[]>[] results = new ArrayList[]{guardsSeen, tracesSeen};

        return results;
    }

    /**
     * @param tilesToRunAwayFrom: tiles with guards or traces intruder wants to run away from
     * @return neighbour that is furthest from all guards or traces
     */
    public int[] runAway(ArrayList<int[]> tilesToRunAwayFrom){
        // get the furthest neighbour from all guards
        int[][] neighbours = agent.getAllNeighbours();

        double highestDistance = Double.MIN_VALUE;
        int[] furthestNeighbour = {,};

        for(int[] neighbour : neighbours){
            double sumOfDistances = 0;
            for(int[] guard : tilesToRunAwayFrom){
                sumOfDistances += RewardTable.distanceBetweenPoints(neighbour[0], neighbour[1], guard[0], guard[1]);
            }
            if(sumOfDistances > highestDistance){
                highestDistance = sumOfDistances;
                furthestNeighbour = neighbour;
            }
        }
        return furthestNeighbour;
    }

    public LearnerAgent getAgent() {
        return agent;
    }

    public void setAgent(LearnerAgent agent) {
        this.agent = agent;
    }

    public QTable getqTable() {
        return qTable;
    }

    public void setqTable(QTable qTable) {
        this.qTable = qTable;
    }

    public RewardTable getRewardTable() {
        return rewardTable;
    }

    public void setRewardTable(RewardTable rewardTable) {
        this.rewardTable = rewardTable;
    }

    public EMTable getEmTable() {
        return emTable;
    }

    public void setEmTable(EMTable emTable) {
        this.emTable = emTable;
    }

    public int[] makeIntruderData(int newX, int newY, Intruder i){

        // angle
        int angle = (int) i.getAngleDeg();

        // sees trace
        int seesTrace = 0;
        if(i.isSeesTrace())
            seesTrace = 1;

        // sees guard
        int seesGuard = 0;
        if(i.isSeesGuard())
            seesGuard = 1;

        // hears yell
        int hearsYell = 0;
        if(i.hearsYell())
            hearsYell = 1;

        // complete data
        return new int[]{newX, newY, angle, seesTrace, seesGuard, hearsYell};
    }

    public int[] makeGuardData(int newX, int newY, Guard g){

        // angle
        int angle = (int) g.getAngleDeg();

        // sees trace
        int seesTrace = 0;
        if(g.seesTrace)
            seesTrace = 1;

        // sees intruder
        int seesIntruder = 0;
        if(g.chasingIntruder)
            seesIntruder = 1;

        // hears yell
        int hearsYell = 0;
        if(g.hearsYell())
            hearsYell = 1;

        // complete data
        return new int[]{newX, newY, angle, seesTrace, seesIntruder, hearsYell};
    }
}
