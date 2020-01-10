package timshell;

import battlecode.common.*;

import java.util.*;

class Utils {
    static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static final int MAX_DISTANCE2 = square(GameConstants.MAP_MAX_HEIGHT) + square(GameConstants.MAP_MAX_WIDTH);

    static final int POLLUTION_LIMIT = 8000;
    static final int POLLUTION_PENALTY_LIMIT = 50;

    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    static double calcPollutionPenalty(int pollution) {
        return (-1) * square(pollution / (double)POLLUTION_LIMIT) * POLLUTION_PENALTY_LIMIT;
    }

    static double square(double x) {
        return x * x;
    }

    static int square(int x) {
        return x * x;
    }
}

class LocationInfo {
    enum InfoType {
        SOUP, REFINERY, ENEMY
    }

    MapLocation location;
    InfoType infoType;
    Object infoData;
    int roundNo;

    LocationInfo(MapLocation location, InfoType type, Object data, int roundNo) {
        this.location = location;
        this.infoType = type;
        this.infoData = data;
        this.roundNo = roundNo;
    }

    boolean sameLocation(LocationInfo o) {
        return location.equals(o.location);
    }
}

abstract class Robot {
    int roundCount;
    RobotController rc;

    Robot(RobotController rc) {
        this.rc = rc;
        this.roundCount = rc.getRoundNum();
    }

    public void update() throws GameActionException {
        roundCount++;
        preTurn();
        onTurn();
        postTurn();
    }

    List<LocationInfo> getSoupLocationsRect(MapLocation bottomLeft, int w, int h) {
        List<LocationInfo> locations = new ArrayList<>();
        for (int dx = 0; dx <= w; ++dx) {
            for (int dy = 0; dy <= h; ++dy) {
                MapLocation pos = bottomLeft.translate(dx, dy);
                if (rc.canSenseLocation(pos)) {
                    int soup = 0;
                    try {
                        soup = rc.senseSoup(pos);
                    } catch (GameActionException e) {
                        e.printStackTrace();
                    }
                    if (soup > 0) {
                        locations.add(new LocationInfo(pos, LocationInfo.InfoType.SOUP, soup, roundCount));
                    }
                }
            }
        }
        return locations;
    }

    public int countSoupRect(MapLocation bottomLeft, int w, int h) {
        int soup = 0;
        for (LocationInfo pos : getSoupLocationsRect(bottomLeft, w, h)) {
            soup += (int)pos.infoData;
        }
        return soup;
    }

    public int countSoupRadius(MapLocation center, int radius2) {
        int r = radius2 / 2;
        return countSoupRect(center.translate(-r, -r), radius2, radius2);
    }

    public void preTurn() throws GameActionException { }

    public abstract void onTurn() throws GameActionException;

    public void postTurn() throws GameActionException {
        // System.out.format("BYTECODES LEFT: %d%n", Clock.getBytecodesLeft());
    }
}

class HQRobot extends Robot {

    HQRobot(RobotController rc) {
        super(rc);
    }

    @Override
    public void onTurn() throws GameActionException {
        int soup = rc.getTeamSoup();

        if (shouldBuildMiner(soup)) {
            for (Direction direction : Direction.allDirections()) {
                if (rc.canBuildRobot(RobotType.MINER, direction)) {
                    rc.buildRobot(RobotType.MINER, direction);
                    return;
                }
            }
        }
    }

    private boolean shouldBuildMiner(int soup) {
        return soup >= RobotType.MINER.cost && rc.isReady();
    }
}

class MinerRobot extends Robot {
    static final int SOUP_CARRY_LIMIT = RobotType.MINER.soupLimit;

    MapLocation HQLocation;
    List<LocationInfo> locations = new LinkedList<>();
    List<MapLocation> soupLocations = new LinkedList<>();

    int sensorRange;
    MapLocation curPos;

    MinerRobot(RobotController rc) {
        super(rc);

        // Takoj lahko najdemo kje se nahaja HQ, ker samo HQ lahko zgradi Minerje.
        for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (r.type == RobotType.HQ) {
                this.HQLocation = r.getLocation();
            }
        }
        locations.add(new LocationInfo(HQLocation, LocationInfo.InfoType.REFINERY, "HQ", roundCount));
        System.out.format("HQ: %s%n", HQLocation);
    }

    @Override
    public void preTurn() throws GameActionException {
        super.preTurn();

        curPos = rc.getLocation();
        sensorRange = rc.getCurrentSensorRadiusSquared();

        /*
        List<LocationInfo> soupLocations = getSoupLocationsRect(
                curPos.translate(-sensorRange / 2, -sensorRange / 2), sensorRange, sensorRange);
        List<LocationInfo> toAdd = new LinkedList<>();
        for (LocationInfo soupInfo : soupLocations) {
            boolean found = false;
            for (LocationInfo info : locations) {
                if (info.sameLocation(soupInfo) && info.infoType == LocationInfo.InfoType.SOUP) {
                    info.infoData = soupInfo.infoData;
                    info.roundNo = roundCount;
                    found = true;
                    break;
                }
            }
            if (!found) {
                toAdd.add(soupInfo);
            }
        }
        locations.addAll(toAdd);
        */
    }

    @Override
    public void onTurn() throws GameActionException {
        if (tryMining()) return;
        if (tryDepositSoup()) return;

        if (shouldRefineSoup()) {
            moveToRefinery();
        } else {
            explore();
        }

        /*
        double moveScore = -100000;
        Direction moveDir = Utils.randomDirection();
        for (Direction dir : Utils.directions) {
            double score = calcMoveScore(dir);
            if (score > moveScore) {
                moveDir = dir;
                moveScore = score;
            }
        }
        if (moveScore > 0) {
            System.out.format("MOVE SCORE: %f%n", moveScore);
            rc.move(moveDir);
        }
        */
    }

    private boolean explore() {
        MapLocation soupLoc = findClosestSoup();
        if (soupLoc != null) {
            return moveTowards(soupLoc);
        }
        for (int i = 0; i < 8; ++i) {
            if (tryMove(Utils.randomDirection()))
                return true;
        }
        return false;
    }

    private MapLocation findClosestSoup() {
        MapLocation closest = null;
        int minDst = Utils.MAX_DISTANCE2;
        for (MapLocation location : soupLocations) {
            int dst = location.distanceSquaredTo(curPos);
            if (dst < minDst) {
                minDst = dst;
                closest = location;
            }
        }
        return closest;
    }

    private MapLocation findClosestRefinery() {
        MapLocation closest = HQLocation;
        int minDst = Utils.MAX_DISTANCE2;
        for (LocationInfo locInfo : locations) {
            if (locInfo.infoType == LocationInfo.InfoType.REFINERY) {
                int dst = locInfo.location.distanceSquaredTo(curPos);
                if (dst < minDst) {
                    minDst = dst;
                    closest = locInfo.location;
                }
            }
        }
        return closest;
    }

    private boolean tryMove(Direction dir) {
        try {
            if (rc.canMove(dir) && !rc.senseFlooding(curPos.add(dir))) {
                rc.move(dir);
                return true;
            }
        } catch (GameActionException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean moveTowards(MapLocation pos) {
        Direction dir = curPos.directionTo(pos);
        return tryMove(dir);
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
                try {
                    rc.depositSoup(dir, rc.getSoupCarrying());
                    return true;
                } catch (GameActionException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    boolean tryMining() {
        for (Direction dir : Utils.directions) {
            if (rc.canMineSoup(dir)) {
                try {
                    rc.mineSoup(dir);
                    return true;
                } catch (GameActionException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private double calcMoveScore(Direction dir) {
        if (!rc.isReady() || !rc.canMove(dir))
            return -100;

        MapLocation nextPos = curPos.add(dir);
        // TODO exploration factor
        int pollution = 50;

        try {
            pollution = rc.sensePollution(nextPos);
        } catch (GameActionException e) {
            e.printStackTrace();
        }

        double score = 50;
        try {
            if (rc.senseFlooding(nextPos)) {
                score -= 50;
            }
        } catch (GameActionException e) {
            e.printStackTrace();
        }
        // TODO delta pollution
        score += Utils.calcPollutionPenalty(pollution);

        // LOGIKA ZA SOUP
        int soupCarrying = rc.getSoupCarrying();
        int soupInRadius = countSoupRadius(nextPos, sensorRange / 2);
        System.out.format("SOUP IN RADIUS: %s -> %s%n", nextPos, soupInRadius);
        score += soupInRadius * 0.1;

        // Treba je iti do rafinerije ...
        if (soupCarrying >= SOUP_CARRY_LIMIT * 0.95) {
           for (LocationInfo location : locations) {
               if (location.infoType == LocationInfo.InfoType.REFINERY) {
                   Direction targetDir = curPos.directionTo(location.location);
                   if (targetDir == dir) {
                       score += 100;
                   }
               }
           }
        }

        return score;
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
