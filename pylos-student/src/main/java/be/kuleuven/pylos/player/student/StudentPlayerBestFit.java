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

public class StudentPlayerBestFit extends PylosPlayer {
    private PylosGameIF currentGame;
    private PylosBoard currentBoard;

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
        final PylosLocation fullSquareLocation = getSquareLocation(allPossibleLocations, player);
        if (fullSquareLocation != null) {
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

    private void placeReserveSphere(List<PylosLocation> allPossibleLocations) {
        PylosSphere reserveSphere = currentBoard.getReserve(this);
        PylosLocation location = allPossibleLocations.size() == 1 ? allPossibleLocations.get(0) : allPossibleLocations.get(getRandom().nextInt(allPossibleLocations.size() - 1));
        currentGame.moveSphere(reserveSphere, location);
    }

    /**
     * @param allPossibleLocations
     * @param player
     * @return first location that forms square player colour
     */
    private PylosLocation getSquareLocation(List<PylosLocation> allPossibleLocations, PylosPlayer player) {
        for (PylosLocation pylosLocation : allPossibleLocations) {
            for (PylosSquare pylosSquare : pylosLocation.getSquares()) {
                if (pylosSquare.getInSquare(player) == 3) {
                    return pylosLocation;
                }
            }
        }
        return null;
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
        } else currentGame.removeSphere(sphere);
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
