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
// Remove second sphere only when it doesn't give opponent opportunity to make square of
// if it doesn't break our opportunity to create square \done

// Ideas for a scoring function:
// - My reserve spheres - your reserve spheres \done
// - My movable spheres - your movable spheres (maximize nr. of lvl up moves)
// - Only finish square when opp. will block otherwise or when you can remove 2 spheres

// TODO: Ranking function for moves with equal score (number of spheres in square, possibility
//  for lvl up, ...)
// TODO: Other ideas...
// TODO: Implement removeSphere & pass in minimax \done
// TODO: When you remove the 4th sphere of your own square and make the square again, you con
//  remove 2 spheres again


public class StudentPlayerBestFit extends PylosPlayer {
    private PylosGameIF currentGame;
    private PylosBoard currentBoard;
    private PylosGameSimulator simulator;
    private final int DEPTH = 2;
    private int currentDepth = 0;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        currentGame = game;
        currentBoard = board;
        final List<PylosLocation> allPossibleLocations = getAllPossibleLocations();
        Collections.shuffle(allPossibleLocations);
        if (makeSquare(this, allPossibleLocations)) return;
        else if (makeSquare(this.OTHER, allPossibleLocations)) return;
        else if (doLevelUp(allPossibleLocations)) return;
        miniMax();
    }

    // First call to miniMax, this method will save the best found move
    public void miniMax() {
        simulator = new PylosGameSimulator(currentGame.getState(), PLAYER_COLOR, currentBoard);
        PylosGameState prevState = simulator.getState();
        PylosSphere bestSphere = null;
        PylosLocation bestLocation = null;
        double bestScore = -9999;

        List<PylosSphere> movableSpheres = getRemovableSpheres(this);
        movableSpheres.add(currentBoard.getReserve(this));
        for (PylosSphere sphere : movableSpheres) {
            PylosLocation prevLocation = sphere.getLocation();
            for (PylosLocation location : getPossibleLocations(sphere)) {
                simulator.moveSphere(sphere, location);
                double score = miniMaxRec();
                if (score > bestScore) {
                    bestScore = score;
                    bestLocation = location;
                    bestSphere = sphere;
                }
                if (prevLocation == null) simulator.undoAddSphere(sphere, prevState, PLAYER_COLOR);
                else simulator.undoMoveSphere(sphere, prevLocation, prevState, PLAYER_COLOR);
            }
        }

        currentGame.moveSphere(bestSphere, bestLocation);
    }

    public double miniMaxRec() {
        if (currentDepth == DEPTH) return eval();

        PylosGameState prevState = simulator.getState();
        double score;
        currentDepth++;
        switch (prevState) {
            case MOVE:
                score = doMoveSim();
                break;
            case REMOVE_FIRST:
                score = doRemoveSim();
                break;
            case REMOVE_SECOND:
                score = doRemoveOrPassSim();
                break;
            case COMPLETED:
            case DRAW:
                score = eval();
                break;
            default:
                throw new IllegalStateException("Game state is: " + prevState);
        }
        currentDepth--;
        return score;
    }

    private double doMoveSim() {
        boolean maximizingPlayer = PLAYER_COLOR == simulator.getColor();
        double bestScore = (maximizingPlayer) ? -9999 : 9999;
        PylosPlayer player = (maximizingPlayer) ? this : OTHER;
        PylosGameState prevState = simulator.getState();
        PylosPlayerColor prevColor = player.PLAYER_COLOR;
        List<PylosSphere> movableSpheres = getRemovableSpheres(player);
        movableSpheres.add(currentBoard.getReserve(player));
        for (PylosSphere sphere : movableSpheres) {
            for (PylosLocation location : getPossibleLocations(sphere)) {
                PylosLocation prevLocation = sphere.getLocation();
                simulator.moveSphere(sphere, location);
                double nextScore = miniMaxRec();
                if (maximizingPlayer && nextScore > bestScore || !maximizingPlayer && nextScore < bestScore) {
                    bestScore = nextScore;
                }
                if (prevLocation == null) simulator.undoAddSphere(sphere, prevState, prevColor);
                else simulator.undoMoveSphere(sphere, prevLocation, prevState, prevColor);
            }
        }
        return bestScore;
    }

    // Returns list of locations where sphere can be placed
    // spheres already on the board can not be placed on the square they currently occupy
    public List<PylosLocation> getPossibleLocations(PylosSphere sphere) {
        List<PylosLocation> locations = getAllPossibleLocations();
        if (sphere.getLocation() != null) {
            for (PylosSquare ps : sphere.getLocation().getSquares()) {
                locations.remove(ps.getTopLocation());
            }
            locations.removeIf(location -> sphere.getLocation().Z >= location.Z);
        }
        return locations;
    }

    /**
     * @param player
     * @param allPossibleLocations
     * @return successfully made/blocked square or not
     */
    private boolean makeSquare(PylosPlayer player, List<PylosLocation> allPossibleLocations) {
        final List<PylosLocation> fullSquareLocations =
                getSquareLocations(allPossibleLocations, player);
        if (!fullSquareLocations.isEmpty()) {
            PylosLocation fullSquareLocation =
                    fullSquareLocations.get((int) (Math.random() * fullSquareLocations.size()));
            // Try to move sphere that is already on board
            PylosSphere sphere = getMovableSphereToLocation(fullSquareLocation);
            // Use reserve sphere
            if (sphere == null) sphere = currentBoard.getReserve(this);
            currentGame.moveSphere(sphere, fullSquareLocation);
            return true;
        }
        return false;
    }

    /**
     * Only move sphere up when it wasn't blocking a square of the opponent
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
        simulator = new PylosGameSimulator(currentGame.getState(), this.PLAYER_COLOR, currentBoard);
        PylosSphere reserveSphere = currentBoard.getReserve(this);
        PylosLocation bestLocation = null;

        double maxScore = -9999;
        for (PylosLocation location : allPossibleLocations) {
            simulator.moveSphere(reserveSphere, location);
            //evaluation
            double score = eval();
            if (score > maxScore) {
                bestLocation = location;
                maxScore = score;
            }
            simulator.undoAddSphere(reserveSphere, currentGame.getState(), this.PLAYER_COLOR);
        }
        currentGame.moveSphere(reserveSphere, bestLocation);
    }

    // Emphasizes preserving the spheres of our player
    private double eval() {
        double reserveScore = (-30 * (15 - currentBoard.getReservesSize(this))) + (10 * (15 - currentBoard.getReservesSize(this.OTHER)));
        double moveScore = 0;
        List<PylosLocation> allPossibleLocations = getAllPossibleLocations();
        moveScore += getMakeSquaresSimulator(this, allPossibleLocations);
        moveScore += doLevelUpSimulator(allPossibleLocations);
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
        PylosSphere moveUpSphere;
        int count = 0;
        for (PylosSphere sphere : currentBoard.getSpheres(this)) {
            if (!opportunityLocations.contains(sphere.getLocation())) {
                for (PylosLocation location : allPossibleLocations) {
                    moveUpSphere = getMovableSphereToLocation(location);
                    if (moveUpSphere != null) count++;
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
    private List<PylosLocation> getSquareLocations(List<PylosLocation> allPossibleLocations,
                                                   PylosPlayer player) {
        List<PylosLocation> squareLocations = new ArrayList<>();
        for (PylosLocation pylosLocation : allPossibleLocations) {
            for (PylosSquare pylosSquare : pylosLocation.getSquares()) {
                if (pylosSquare.getInSquare(player) == 3) squareLocations.add(pylosLocation);
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
            if (!ps.isReserve() && ps.canMoveTo(location)) return ps;
        }
        return null;
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        currentGame = game;
        currentBoard = board;
        simulator = new PylosGameSimulator(currentGame.getState(), PLAYER_COLOR, currentBoard);
        final List<PylosSphere> removableSpheres = getRemovableSpheres(this);
        double bestScore = -9999;
        PylosSphere bestSphereToRemove = null;
        for (PylosSphere sphereToRemove : removableSpheres) {
            PylosLocation prevLocation = sphereToRemove.getLocation();
            simulator.removeSphere(sphereToRemove);
            double result = miniMaxRec();
            if (result > bestScore) {
                bestSphereToRemove = sphereToRemove;
                bestScore = result;
            }
            simulator.undoRemoveFirstSphere(sphereToRemove, prevLocation, PylosGameState.REMOVE_FIRST, PLAYER_COLOR);
        }
        game.removeSphere(bestSphereToRemove);
    }

    public double doRemoveSim() {
        PylosPlayerColor simulatorColor = simulator.getColor();
        PylosPlayer simulatorPlayer = (simulatorColor == PLAYER_COLOR) ? this : OTHER;
        final List<PylosSphere> removableSpheres = getRemovableSpheres(simulatorPlayer);
        double bestScore = (simulatorColor == PLAYER_COLOR) ? -9999 : 9999;
        for (PylosSphere sphereToRemove : removableSpheres) {
            PylosLocation prevLocation = sphereToRemove.getLocation();
            simulator.removeSphere(sphereToRemove);
            double result = miniMaxRec();
            if (simulatorColor == PLAYER_COLOR && result > bestScore) {
                bestScore = result;
            } else if (simulatorColor != PLAYER_COLOR && result < bestScore) {
                bestScore = result;
            }
            simulator.undoRemoveFirstSphere(sphereToRemove, prevLocation, PylosGameState.REMOVE_FIRST, simulatorColor);
        }
        return bestScore;
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        currentGame = game;
        currentBoard = board;
        simulator = new PylosGameSimulator(currentGame.getState(), PLAYER_COLOR, currentBoard);
        final List<PylosSphere> removableSpheres = getRemovableSpheres(this);
        double bestScoreRemove = -9999;
        PylosSphere bestSphereToRemove = null;
        for (PylosSphere sphereToRemove : removableSpheres) {
            PylosLocation prevLocation = sphereToRemove.getLocation();
            simulator.removeSphere(sphereToRemove);
            double result = miniMaxRec();
            if (result > bestScoreRemove) {
                bestSphereToRemove = sphereToRemove;
                bestScoreRemove = result;
            }
            simulator.undoRemoveSecondSphere(sphereToRemove, prevLocation, PylosGameState.REMOVE_SECOND, PLAYER_COLOR);
        }
        simulator.pass();
        double bestScorePass = miniMaxRec();
        simulator.undoPass(PylosGameState.REMOVE_SECOND, PLAYER_COLOR);
        if (bestScorePass > bestScoreRemove || bestSphereToRemove == null) {
            game.pass();
        } else {
            game.removeSphere(bestSphereToRemove);
        }
    }

    private double doRemoveOrPassSim() {
        PylosPlayerColor simulatorColor = simulator.getColor();
        PylosPlayer simulatorPlayer = (simulatorColor == PLAYER_COLOR) ? this : OTHER;
        final List<PylosSphere> removableSpheres = getRemovableSpheres(simulatorPlayer);
        double bestScore = (simulatorColor == PLAYER_COLOR) ? -9999 : 9999;

        for (PylosSphere sphereToRemove : removableSpheres) {
            PylosLocation prevLocation = sphereToRemove.getLocation();
            simulator.removeSphere(sphereToRemove);
            double result = miniMaxRec();
            if (simulatorColor == PLAYER_COLOR && result > bestScore) {
                bestScore = result;
            } else if (simulatorColor != PLAYER_COLOR && result < bestScore) {
                bestScore = result;
            }
            simulator.undoRemoveSecondSphere(sphereToRemove, prevLocation, PylosGameState.REMOVE_SECOND, simulatorColor);
        }
        simulator.pass();
        double result = miniMaxRec();
        if (simulatorColor == PLAYER_COLOR && result > bestScore) {
            bestScore = result;
        } else if (simulatorColor != PLAYER_COLOR && result < bestScore) {
            bestScore = result;
        }
        simulator.undoPass(PylosGameState.REMOVE_SECOND, simulatorColor);
        return bestScore;
    }

    /**
     * @param removableSpheres
     * @return sphere that doesn't give opponent opportunity to make square and doesn't break our
     * opportunity to create square
     */
    // TODO make more random & check if opponent isn't already blocking the square
    private PylosSphere getOptimalRemovableSphere(List<PylosSphere> removableSpheres) {
        for (PylosSphere ps : removableSpheres) {
            for (PylosSquare pylosSquare : ps.getLocation().getSquares()) {
                final int ownSpheres = pylosSquare.getInSquare(this);
                final int opponentSpheres = pylosSquare.getInSquare(this.OTHER);
                if (opponentSpheres != 3 && ownSpheres != 3) return ps;
            }
        }
        return null;
    }

    /**
     * @param removableSpheres
     * @return spheres that doesn't give opponent opportunity to make square and doesn't break our
     * opportunity to create square
     */
    private List<PylosSphere> getOptimalRemovableSpheres(List<PylosSphere> removableSpheres) {
        List<PylosSphere> optimalRemovableSpheres = new ArrayList<>();
        for (PylosSphere ps : removableSpheres) {
            for (PylosSquare pylosSquare : ps.getLocation().getSquares()) {
                final int ownSpheres = pylosSquare.getInSquare(this);
                final int opponentSpheres = pylosSquare.getInSquare(this.OTHER);
                if (opponentSpheres != 3 && ownSpheres != 3) optimalRemovableSpheres.add(ps);
            }
        }
        return optimalRemovableSpheres;
    }

    private List<PylosSphere> getRemovableSpheres(PylosPlayer player) {
        final List<PylosSphere> removableSpheres = new ArrayList<>();
        for (PylosSphere ps : currentBoard.getSpheres(player)) {
            if (!ps.isReserve() && !ps.getLocation().hasAbove()) {
                removableSpheres.add(ps);
            }
        }
        return removableSpheres;
    }

    private List<PylosLocation> getAllPossibleLocations() {
        final List<PylosLocation> allPossibleLocations = new ArrayList<>();
        for (PylosLocation bl : currentBoard.getLocations()) {
            if (bl.isUsable()) allPossibleLocations.add(bl);
        }
        return allPossibleLocations;
    }
}
