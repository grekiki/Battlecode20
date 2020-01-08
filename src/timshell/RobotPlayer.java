package timshell;

import battlecode.common.*;

import java.util.*;

class Utils {
    static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
                                     Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static final int MAX_DISTANCE2 = square(GameConstants.MAP_MAX_HEIGHT) + square(GameConstants.MAP_MAX_WIDTH);

    static int MAP_WIDTH;
    static int MAP_HEIGHT;

    static void init(RobotController rc) {
        MAP_WIDTH = rc.getMapWidth();
        MAP_HEIGHT = rc.getMapHeight();
    }

    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static int square(int x) {
        return x * x;
    }

    static boolean isOnMap(MapLocation pos) {
        return pos.x >= 0 && pos.x < MAP_WIDTH && pos.y >= 0 && pos.y < MAP_HEIGHT;
    }

    static int clamp(int v, int min, int max) {
        return Math.max(Math.min(v, max), min);
    }

    static MapLocation findClosestLocation(MapLocation curPos, Collection<MapLocation> locations) {
        MapLocation closest = null;
        int minDst = Utils.MAX_DISTANCE2;
        for (MapLocation location : locations) {
            int dst = location.distanceSquaredTo(curPos);
            if (dst < minDst) {
                minDst = dst;
                closest = location;
            }
        }
        return closest;
    }
}

class MapCell {
    int timesVisited = 0;
    int soupCount = 0;
    int pollutionLevel = 0;
    int elevation = 0;
    boolean flooded = false;
    int lastTurnSeen = -1;
    RobotType building = null;
}

abstract class Robot {
    int roundCount;
    RobotController rc;

    MapCell[][] mapState;  // [x][y] indeksiranje

    Robot(RobotController rc) {
        this.rc = rc;
        this.roundCount = rc.getRoundNum();
        System.out.format("%d %d%n",Utils.MAP_WIDTH, Utils.MAP_HEIGHT);
        this.mapState = new MapCell[Utils.MAP_WIDTH][Utils.MAP_HEIGHT];
        for (int x = 0; x < mapState.length; ++x) {
            for (int y = 0; y < mapState[x].length; ++y) {
                mapState[x][y] = new MapCell();
            }
        }
    }

    public void update() throws GameActionException {
        roundCount++;
        preTurn();
        onTurn();
        postTurn();
    }

    public void printMapState() {
        for (int y = mapState[0].length - 1; y >= 0; --y) {
            for (int x = 0; x < mapState.length; ++x) {
                MapCell cell = mapState[x][y];
                if (cell.flooded) {
                    System.out.print("F");
                } else if (cell.building != null) {
                    System.out.print("B");
                } else if (cell.soupCount > 0) {
                    System.out.print("S");
                } else {
                    System.out.print(cell.timesVisited);
                }
            }
            System.out.println();
        }
    }

    public boolean senseFlooding(MapLocation pos) throws GameActionException {
        boolean flooded = rc.senseFlooding(pos);
        updateCell(pos, "FLOOD", flooded);
        return flooded;
    }

    public int senseSoup(MapLocation pos) throws GameActionException {
        int soup = rc.senseSoup(pos);
        updateCell(pos, "SOUP", soup);
        return soup;
    }

    public MapCell updateCell(MapLocation pos, String key, Object value) {
        MapCell cell = mapState[pos.x][pos.y];
        cell.lastTurnSeen = roundCount;
        switch (key) {
            case "VISIT": cell.timesVisited++; break;
            case "SOUP": cell.soupCount = (int) value; break;
            case "ELEVATION": cell.elevation = (int) value; break;
            case "BUILDING": cell.building = (RobotType) value; break;
            case "FLOOD": cell.flooded = (boolean) value; break;
            case "POLLUTION": cell.pollutionLevel = (int) value; break;
        }
        return cell;
    }

    public MapCell updateSenseCell(MapLocation pos) {
        if (!rc.canSenseLocation(pos)) return null;
        try {
            updateCell(pos, "SOUP", rc.senseSoup(pos));
            updateCell(pos, "ELEVATION", rc.senseElevation(pos));
            updateCell(pos, "FLOOD", rc.senseFlooding(pos));
            updateCell(pos, "POLLUTION", rc.sensePollution(pos));
        } catch (GameActionException e) {
            //e.printStackTrace();
            System.out.println("Cannot sense " + pos);
        }
        return mapState[pos.x][pos.y];
    }

    void floodFillScanSoup(MapLocation pos) {
        if (!rc.canSenseLocation(pos) || mapState[pos.x][pos.y].lastTurnSeen == roundCount) return;

        MapCell c = updateSenseCell(pos);
        if (c == null || c.soupCount <= 0) {
            return;
        }

        for (Direction dir : Utils.directions) {
            if (Clock.getBytecodesLeft() < 1000) return;
            MapLocation p = pos.add(dir);
            c = updateSenseCell(p);
            if (c != null && c.soupCount > 0) {
                floodFillScanSoup(p);
            }
        }
    }

    public void updateMapState() {
        int range = rc.getCurrentSensorRadiusSquared();
        MapLocation pos = rc.getLocation();
        for (int i = (int) (Math.random() * range / 2); i < range; ++i) {
            MapLocation[] scan = pc.range[i];
            for (MapLocation mm : scan) {
                if (Clock.getBytecodesLeft() < 1000) return;
                MapLocation m = new MapLocation(pos.x + mm.x, pos.y + mm.y);
                floodFillScanSoup(m);
            }
        }
    }

    public void preTurn() throws GameActionException { }

    public abstract void onTurn() throws GameActionException;

    public void postTurn() throws GameActionException {
        System.out.format("BYTECODES LEFT: %d%n", Clock.getBytecodesLeft());
        updateMapState();
    }
}

class HQRobot extends Robot {

    Direction prevMinerDirection = Utils.directions[0];

    HQRobot(RobotController rc) {
        super(rc);
    }

    @Override
    public void onTurn() throws GameActionException {
        int soup = rc.getTeamSoup();

        if (shouldBuildMiner(soup)) {
            for (int i = 0; i < 8; ++i) {
                Direction direction = prevMinerDirection.rotateRight();
                if (rc.canBuildRobot(RobotType.MINER, direction)) {
                    rc.buildRobot(RobotType.MINER, direction);
                    prevMinerDirection = direction;
                    return;
                }
            }
        }
    }

    @Override
    public void postTurn() throws GameActionException {
        super.postTurn();
    }

    private boolean shouldBuildMiner(int soup) {
        return soup >= RobotType.MINER.cost && rc.isReady() && Math.random() < 0.5;
    }
}

class MinerRobot extends Robot {
    static final int SOUP_CARRY_LIMIT = RobotType.MINER.soupLimit;

    MapLocation HQLocation;
    List<MapLocation> refineryLocations = new LinkedList<>();
    Set<MapLocation> soupLocations = new HashSet<>();

    MapLocation curPos;
    Direction initialDirection;
    int directionAttempts = 0;

    MinerRobot(RobotController rc) {
        super(rc);

        // Takoj lahko najdemo kje se nahaja HQ, ker samo HQ lahko zgradi Minerje.
        for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (r.type == RobotType.HQ) {
                this.HQLocation = r.getLocation();
                break;
            }
        }
        refineryLocations.add(HQLocation);
        updateCell(HQLocation, "BUILDING", RobotType.HQ);
        System.out.format("HQ: %s%n", HQLocation);

        initialDirection = rc.getLocation().directionTo(HQLocation).opposite();
    }

    @Override
    public void preTurn() throws GameActionException {
        super.preTurn();

        curPos = rc.getLocation();
        updateCell(curPos, "VISIT", 1);

        // System.out.println(refineryLocations);
        System.out.println(soupLocations);
    }

    @Override
    public void onTurn() throws GameActionException {
        if (tryMining()) return;
        if (tryDepositSoup()) return;

        if (shouldRefineSoup()) {
            if (moveToRefinery()) return;
            if (explore(0.98)) return;
        }

        explore(Math.random());
    }

    @Override
    public void postTurn() throws GameActionException {
        int range = rc.getCurrentSensorRadiusSquared();
        for (MapLocation soupLoc : soupLocations) {
            if (Clock.getBytecodesLeft() < 800) break;
            if (curPos.isWithinDistanceSquared(soupLoc, range)) {
                floodFillScanSoup(soupLoc);
            }
        }
        for (MapLocation refineryLoc : refineryLocations) {
            if (Clock.getBytecodesLeft() < 800) break;
            if (curPos.isWithinDistanceSquared(refineryLoc, range)) {
                updateSenseCell(refineryLoc);
            }
        }
        for (RobotInfo robot : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (Clock.getBytecodesLeft() < 800) break;
            updateCell(robot.getLocation(), "VISIT", 1);
        }
        super.postTurn();
    }

    @Override
    public MapCell updateCell(MapLocation pos, String key, Object value) {
        MapCell cell = mapState[pos.x][pos.y];
        switch (key) {
            case "SOUP":
                int soup = (int) value;
                if (soup > 0) {
                    soupLocations.add(pos);
                } else {
                    soupLocations.remove(pos);
                }
                break;
            case "BUILDING":
                RobotType building = (RobotType) value;
                if (cell.building == null && building == RobotType.REFINERY) {
                    refineryLocations.add(pos);
                }
                break;
        }
        return super.updateCell(pos, key, value);
    }

    public boolean canMineSoup(Direction dir) throws GameActionException {
        boolean canMine = rc.canMineSoup(dir);
        senseSoup(curPos.add(dir));
        return canMine;
    }

    private MapLocation closestUnexplored() {
        boolean[][] marked = new boolean[Utils.MAP_WIDTH][Utils.MAP_HEIGHT];
        Queue<MapLocation> q = new LinkedList<>();
        q.add(curPos);
        while (!q.isEmpty()) {
            MapLocation p = q.remove();
            if (Utils.isOnMap(p) && mapState[p.x][p.y].timesVisited == 0) { // && !senseFlooding(p)) {
                return p;
            }
            marked[p.x][p.y] = true;
            for (Direction dir : Utils.directions) {
                MapLocation pp = p.add(dir);
                if (Utils.isOnMap(pp) && !marked[pp.x][pp.y]) {
                    q.add(pp);
                }
            }
        }

        return null;
    }

    private boolean explore(double explorationFactor) {
        if (explorationFactor < 0.88) {
            MapLocation soupLoc = findClosestSoup();
            if (soupLoc != null) {
                if (moveTowards(soupLoc)) return true;
            }
        }

        /*
        int mostVisited = -1;
        Direction bestDir = null;
        fior (Direction dir : Utils.directions) {
            MapLocation p = curPos.add(dir);
            if (Utils.isOnMap(p)) {
                int c = mapState[p.x][p.y].timesVisited;
                if (c > mostVisited) {
                    mostVisited = c;
                    bestDir = dir;
                }
            }
        }
        if (bestDir != null)
            if (tryMove(bestDir)) return true;
        */

        /*
        MapLocation closest = closestUnexplored();
        if (closest != null) {
            if (moveTowards(closest)) return true;
        }
        */

        if (initialDirection != null) {
            if (moveTowards(curPos.add(initialDirection))) return true;
            else {
                directionAttempts++;
                if (directionAttempts > 12) {
                    initialDirection = null;
                }
            }
        }

        for (int i = 0, j = (int) (Math.random() * 8); i < 8; ++i, j = (j + 1) % 8) {
            Direction dir = Utils.directions[j];
            MapLocation p = curPos.add(dir);
            if (Utils.isOnMap(p) && mapState[p.x][p.y].timesVisited == 0 && !mapState[p.x][p.y].flooded) {
                if (tryMove(dir)) return true;
            }
        }

        for (int i = 0; i < 4; ++i) {
            if (tryMove(Utils.randomDirection())) return true;
        }

        return false;
    }

    private MapLocation findClosestSoup() {
        return Utils.findClosestLocation(curPos, soupLocations);
    }

    private MapLocation findClosestRefinery() {
        return Utils.findClosestLocation(curPos, refineryLocations);
    }

    private boolean tryMove(Direction dir) {
        try {
            if (rc.canMove(dir) && !senseFlooding(curPos.add(dir))) {
                rc.move(dir);
                updateCell(curPos.add(dir), "VISIT", 1);
                return true;
            }
        } catch (GameActionException e) {
            // e.printStackTrace();
        }
        return false;
    }

    private boolean moveTowards(MapLocation pos) {
        if (pos == null) return false;
        Direction dir = curPos.directionTo(pos);
        if (tryMove(dir)) return true;
        if (tryMove(dir.rotateLeft())) return true;
        if (tryMove(dir.rotateRight())) return true;
        if (tryMove(dir.rotateLeft().rotateLeft())) return true;
        if (tryMove(dir.rotateRight().rotateRight())) return true;
        return false;
    }

    private boolean moveToRefinery() {
        MapLocation closestRefinery = findClosestRefinery();
        return moveTowards(closestRefinery);
    }

    private boolean shouldRefineSoup() {
        return rc.getSoupCarrying() >= SOUP_CARRY_LIMIT * 0.98;
    }

    private boolean tryDepositSoup() {
        for (Direction dir : Utils.directions) {
            if (rc.canDepositSoup(dir)) {
                MapLocation pos = curPos.add(dir);
                if (!pos.equals(HQLocation))
                    updateCell(pos, "BUILDING", RobotType.REFINERY);
                try {
                    rc.depositSoup(dir, rc.getSoupCarrying());
                    return true;
                } catch (GameActionException e) {
                    // e.printStackTrace();
                }
            }
        }
        return false;
    }

    boolean tryMining() {
        for (Direction dir : Direction.allDirections()) {
            try {
                if (canMineSoup(dir)) {
                    rc.mineSoup(dir);
                    return true;
                }
            } catch (GameActionException e) {
                // e.printStackTrace();
            }
        }
        return false;
    }
}

public strictfp class RobotPlayer {
    static RobotController rc;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        RobotType type = rc.getType();
        System.out.println("SPAWN " + type);

        // KONSTANTE JE TREBA VSAKIC ZNOVA IZRACUNATI, KER JE VSAK ROBOT SVOJA INSTANCA!
        Utils.init(rc);

        Robot robot = null;
        switch (type) {
            case HQ: robot = new HQRobot(rc); break;
            case MINER: robot = new MinerRobot(rc); break;
        }

        while (true) {
            if (robot == null) continue;

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                robot.update();
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();
            } catch (Exception e) {
                System.out.println(type + " Exception");
                e.printStackTrace();
            }
        }
    }
}
