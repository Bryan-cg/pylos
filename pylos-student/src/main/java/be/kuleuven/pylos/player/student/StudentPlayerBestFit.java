package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Strategy move:
// try to place sphere to make square with own colours \done
// block square with all colours opponent \done
// Move sphere level up only when doesn't give opponent opportunity to make square \done
// Place reserve sphere on board \done

// Strategy remove:
// Remove second sphere only when it doesn't give opponent opportunity to make square of if it doesn't break our opportunity to create square \done

// TODO: Better performance when we try to add new spheres to middle of board?
// TODO: Scoring function to calculate how good move is?
// TODO: Other ideas...

// TODO: recursive minMax (idea of structure below)
//recSim(lvl){
//    score = 0;
//    if(lvl == 0) score += calcScore();
//    else{
//        for(all moves){
//            simulator.doMove()
//            score += recSim(lvl-1)
//            simulator.undoMove();
//        }
//    }
//    return score;
//}

public class StudentPlayerBestFit extends PylosPlayer {
    private PylosGameIF currentGame;
    private PylosBoard currentBoard;
    private PylosGameSimulator simulator;
    private final int branchDepth = 2;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        // Update current game & board
        currentGame = game;
        currentBoard = board;

        final List<PylosLocation> allPossibleLocations = getAllPossibleLocations();
        Collections.shuffle(allPossibleLocations);
        if (makeSquare(this, allPossibleLocations)) return;
        else if (makeSquare(this.OTHER, allPossibleLocations)) return;
        else if (doLevelUp(allPossibleLocations)) return;
        else placeReserveSphere(allPossibleLocations);
    }

    /**
     * @param player
     * @param allPossibleLocations
     * @return successfully made/blocked square or not
     */
    private boolean makeSquare(PylosPlayer player, List<PylosLocation> allPossibleLocations) {
        final List<PylosLocation> fullSquareLocations = getSquareLocations(allPossibleLocations, player);
        if (!fullSquareLocations.isEmpty()) {
            PylosLocation fullSquareLocation = fullSquareLocations.get((int) (Math.random() * fullSquareLocations.size()));
            // Try to move sphere that is already on board
            PylosSphere sphere = getMovableSphereToLocation(fullSquareLocation);
            // Use reserve sphere
            if (sphere == null)
                sphere = currentBoard.getReserve(this);
            currentGame.moveSphere(sphere, fullSquareLocation);
            return true;
        }
        return false;
    }

    /**
     * Only move up when doesn't give opportunity to opponent for square
     *
     * @return successfully moved sphere to higher level
     */
    private boolean doLevelUp(List<PylosLocation> allPossibleLocations) {
        final List<PylosLocation> opportunityLocations = new ArrayList<>();
        for (PylosSquare psq : currentBoard.getAllSquares()) {
            if (psq.getInSquare(this.OTHER) != 3) {
                opportunityLocations.addAll(Arrays.asList(psq.getLocations()));
            } else {
                opportunityLocations.removeAll(Arrays.asList(psq.getLocations()));
            }
        }
        PylosSphere moveUpSphere = null;
        PylosLocation moveToLocation = null;
        for (PylosSphere sphere : currentBoard.getSpheres(this)) {
            if (!opportunityLocations.contains(sphere.getLocation())) {
                for (PylosLocation location : allPossibleLocations) {
                    moveUpSphere = getMovableSphereToLocation(location);
                    if (moveUpSphere != null) {
                        moveToLocation = location;
                        break;
                    }
                }
                if (moveUpSphere != null) break;
            }
        }
        if (moveUpSphere == null) return false;
        currentGame.moveSphere(moveUpSphere, moveToLocation);
        return true;
    }

    /**
     * Place reserve sphere at square without spheres or random location
     *
     * @param allPossibleLocations
     */
    private void placeReserveSphere(List<PylosLocation> allPossibleLocations) {
        int currentBranch = 0;
        simulator = new PylosGameSimulator(currentGame.getState(), this.PLAYER_COLOR, currentBoard);
        PylosSphere reserveSphere = currentBoard.getReserve(this);
        PylosLocation bestLocation = null;
        while (currentBranch < branchDepth) {
            if (currentBranch % 2 == 0) {
                // White' s turn
            } else {
                // Black's turn
            }
            currentBranch++;
        }

        double maxScore = -9999;
        for (PylosLocation location : allPossibleLocations) {
            simulator.moveSphere(reserveSphere, location);
            //evaluation
            double score = eval();
            if (score > maxScore) {
                bestLocation = location;
            }
            simulator.undoAddSphere(reserveSphere, currentGame.getState(), this.PLAYER_COLOR);
        }
        currentGame.moveSphere(reserveSphere, bestLocation);
    }

    private double eval() {
        double reserveScore = currentBoard.getReservesSize(this) - currentBoard.getReservesSize(this.OTHER);
        double moveScore = 0;
        List<PylosLocation> reserveSpheres = getAllPossibleLocations();
        moveScore += getMakeSquaresSimulator(this.OTHER, reserveSpheres);
        moveScore += doLevelUpSimulator(reserveSpheres);
        return reserveScore + moveScore;
    }

    /**
     * @param player
     * @param allPossibleLocations
     * @return successfully made/blocked square or not
     */
    private double getMakeSquaresSimulator(PylosPlayer player, List<PylosLocation> allPossibleLocations) {
        return getSquareLocations(allPossibleLocations, player).size();
    }

    /**
     * Only move up when doesn't give opportunity to opponent for square
     *
     * @return successfully moved sphere to higher level
     */
    private double doLevelUpSimulator(List<PylosLocation> allPossibleLocations) {
        final List<PylosLocation> opportunityLocations = new ArrayList<>();
        for (PylosSquare psq : currentBoard.getAllSquares()) {
            if (psq.getInSquare(this.OTHER) != 3) {
                opportunityLocations.addAll(Arrays.asList(psq.getLocations()));
            } else {
                opportunityLocations.removeAll(Arrays.asList(psq.getLocations()));
            }
        }
        PylosSphere moveUpSphere = null;
        int count = 0;
        for (PylosSphere sphere : currentBoard.getSpheres(this)) {
            if (!opportunityLocations.contains(sphere.getLocation())) {
                for (PylosLocation location : allPossibleLocations) {
                    moveUpSphere = getMovableSphereToLocation(location);
                    if (moveUpSphere != null) {
                        count++;
                    }
                }
            }
        }
        return count;
    }


    /**
     * @param allPossibleLocations
     * @param player
     * @return Location that allows player to form square
     */
    private List<PylosLocation> getSquareLocations(List<PylosLocation> allPossibleLocations, PylosPlayer player) {
        List<PylosLocation> squareLocations = new ArrayList<>();
        for (PylosLocation pylosLocation : allPossibleLocations) {
            for (PylosSquare pylosSquare : pylosLocation.getSquares()) {
                if (pylosSquare.getInSquare(player) == 3) {
                    squareLocations.add(pylosLocation);
                }
            }
        }
        return squareLocations;
    }

    /**
     * @param location
     * @return sphere on board that can be moved to location or null
     */
    private PylosSphere getMovableSphereToLocation(PylosLocation location) {
        for (PylosSphere ps : currentBoard.getSpheres(this)) {
            if (!ps.isReserve() && ps.canMoveTo(location))
                return ps;
        }
        return null;
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        // Update current game & board
        currentGame = game;
        currentBoard = board;
        final List<PylosSphere> removableSpheres = getRemovableSpheres();
        Collections.shuffle(removableSpheres);
        PylosSphere sphereToRemove;
        if (removableSpheres.size() == 1) {
            sphereToRemove = removableSpheres.get(0);
        } else {
            sphereToRemove = getOptimalRemovableSphere(removableSpheres);
            if (sphereToRemove == null) {
                // temporary solution - remove random sphere
                sphereToRemove = removableSpheres.get(0);
            }
        }
        game.removeSphere(sphereToRemove);
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        // Update current game & board
        currentGame = game;
        currentBoard = board;
        final List<PylosSphere> removableSpheres = getRemovableSpheres();
        PylosSphere sphere = getOptimalRemovableSphere(removableSpheres);
        if (sphere == null) {
            currentGame.pass();
        } else if (!currentGame.removeSphereIsDraw(sphere)) {
            currentGame.removeSphere(sphere);
        } else {
            currentGame.pass();
        }
    }

    /**
     * @param removableSpheres
     * @return sphere that doesn't give opponent opportunity to make square and doesn't break our opportunity to create square
     */
    private PylosSphere getOptimalRemovableSphere(List<PylosSphere> removableSpheres) {
        for (PylosSphere ps : removableSpheres) {
            for (PylosSquare pylosSquare : ps.getLocation().getSquares()) {
                final int ownSpheres = pylosSquare.getInSquare(this);
                final int opponentSpheres = pylosSquare.getInSquare(this.OTHER);
                if (opponentSpheres != 3 && ownSpheres != 3) {
                    return ps;
                }
            }
        }
        return null;
    }

    private List<PylosSphere> getRemovableSpheres() {
        final List<PylosSphere> removableSpheres = new ArrayList<>();
        for (PylosSphere ps : currentBoard.getSpheres(this)) {
            if (!ps.isReserve() && !ps.getLocation().hasAbove()) {
                removableSpheres.add(ps);
            }
        }
        return removableSpheres;
    }

    private List<PylosLocation> getAllPossibleLocations() {
        final List<PylosLocation> allPossibleLocations = new ArrayList<>();
        for (PylosLocation bl : currentBoard.getLocations()) {
            if (bl.isUsable()) {
                allPossibleLocations.add(bl);
            }
        }

        return allPossibleLocations;
    }
}
