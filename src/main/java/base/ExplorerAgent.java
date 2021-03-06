package base;

import Controller.Map;
import Controller.Teleport;
import QLearning.QLearning;

import java.util.ArrayList;
import java.util.Arrays;

import static QLearning.QLearning.*;
import static QLearning.RewardTable.distanceBetweenPoints;
import static base.GameController.*;

public class ExplorerAgent extends Agent{

    public static final byte
            HEIGHT_INDEX = 2,
            WIDTH_INDEX = 3;

    private double[] explorationArea; // {x, y, height, width}
    private final ArrayList<int[]> exploredTiles;

    public ExplorerAgent(int[] position, int angle) {
        super(position, angle);
        exploredTiles = new ArrayList<>();
        exploredTiles.add(new int[]{position[0], position[1]});
    }

    public void makeExplorationMove() {

        int[] nextPosition = this.getPosition();

        if (!isInHisArea()) {
            nextPosition = getTileClosestToArea();
        } else {
            boolean appropriateNeighbourIsFound = false;
            for (int[] neighbour : getAllNeighbours()) {
                // get first one that is In Bounds, Not A Wall and has not been explored
                if (isInHisArea(neighbour) && !isAlreadyExplored(neighbour) && !map.hasWall(neighbour)) {
                    nextPosition = neighbour;
                    appropriateNeighbourIsFound = true;
                    break;
                }
            }
            if (!appropriateNeighbourIsFound) {
                nextPosition = exploredTiles.get(exploredTiles.size() - 2);
                exploredTiles.clear();
            }
        }

        applyNextMove(nextPosition);
    }

    private int[] getTileClosestToArea(){

        int[][] neighbours = getAllNeighbours();

        double smallestDistanceToRegion = Double.MAX_VALUE;
        int[] closestNeighbourToRegion = neighbours[0];

        for(int[] n : neighbours){
            int
                    middleX = (int) (explorationArea[0] + explorationArea[WIDTH_INDEX]/2),
                    middleY = (int) (explorationArea[1] + explorationArea[HEIGHT_INDEX]/2);

            double distance = distanceBetweenPoints(n[0], n[1], middleX, middleY);
            if(distance < smallestDistanceToRegion){
                smallestDistanceToRegion = distance;
                closestNeighbourToRegion = n;
            }
        }

        return closestNeighbourToRegion;
    }

    private boolean isInHisArea(){
        int
                x = getX(), y = getY(),
                startX = (int) explorationArea[0], endX = startX + (int) explorationArea[WIDTH_INDEX],
                startY = (int) explorationArea[1], endY = startY + (int) explorationArea[HEIGHT_INDEX];

        return startX <= x && x < endX
                && startY <= y && y < endY;
    }

    public boolean isInHisArea(int[] position){
        int
                x = position[0], y = position[1],
                startX = (int) explorationArea[0], endX = startX + (int) explorationArea[WIDTH_INDEX],
                startY = (int) explorationArea[1], endY = startY + (int) explorationArea[HEIGHT_INDEX];

        return startX <= x && x < endX
                && startY <= y && y < endY;
    }

    public boolean isAlreadyExplored(int[] position){
        for(int[] t : exploredTiles){
            if(t[0] == position[0] && t[1] == position[1])
                return true;
        }
        return false;
    }

    public void applyNextMove(int[] nextPosition){
        boolean wentThroughTeleport = false;

        // check for teleport
        for(Teleport tp : variables.getPortals()){
            for(int[] p : tp.getPointsIn()){
                if( p[0] == nextPosition[0]  &&  p[1] == nextPosition[1]){
                    wentThroughTeleport = true;
                    nextPosition = tp.getPointOut();
                    setAngleDeg(tp.getDegreeOut());
                    applyMove(nextPosition);
                }
            }
        }

        // if no portal was used
        if(!wentThroughTeleport) {
            updateAngle(nextPosition);
            applyMove(nextPosition);
        }

        // check if is in shaded area
        if(map.getTile(nextPosition).hasShade()){
            this.setVisionRange(variables.getVisionRange()/10);
        }
        else this.setVisionRange(variables.getVisionRange());

        // update vision area
        updateVisionArea();


    }

    public void updateAngle(int[] nextPosition){
        int newX = nextPosition[0], newY = nextPosition[1];
        double angle = this.getAngleDeg();

        if(getX() != newX){
            if(newX == getX()-1)
                angle = 180;
            else angle = 0;
        }

        if(getY() != newY){
            if(newY == getY()-1)
                angle = 270;
            else angle = 90;
        }

        setAngleDeg(angle);
    }

    public void applyMove(int[] nextPosition){
        getTrace().addToTrace(new int[]{getX(), getY()});
        System.out.println("trace added at: "+ getX()+ ", "+getY() );
        this.setPosition(nextPosition[0], nextPosition[1]);
        exploredTiles.add(new int[]{nextPosition[0], nextPosition[1]});
    }

    public void updateVisionArea(){
        this.visionT.clear();
        this.getRayEngine().calculate(this);
        this.visionT = this.getRayEngine().getVisibleTiles(this);
    }

    public void clearExploredTiles(){
        this.exploredTiles.clear();
    }

    public void setExplorationArea(double[] area){
        this.explorationArea = area;
    }

    public double[] getExplorationArea() {
        return explorationArea;
    }

}
