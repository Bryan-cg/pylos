package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// Strategy move:
// try to place sphere to make square with own colours
// block square with all colours opponent


// Strategy remove:
//

public class StudentPlayerBestFit extends PylosPlayer {
    private PylosGameIF currentGame;
    private PylosBoard currentBoard;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        // Update current game & board
        currentGame = game;
        currentBoard = board;

        // Get all usable locations
        final List<PylosLocation> allPossibleLocations = new ArrayList<>();
        for (PylosLocation bl : board.getLocations()) {
            if (bl.isUsable()) {
                allPossibleLocations.add(bl);
            }
        }
        Collections.shuffle(allPossibleLocations);
        // Make square own colours if possible
        if (makeSquare(this, allPossibleLocations)) return;
        // Block square opponent
        else if (makeSquare(this.OTHER, allPossibleLocations)) return;

        //else random move temporary
        PylosSphere reserveSphere = board.getReserve(this);
        PylosLocation location = allPossibleLocations.size() == 1 ? allPossibleLocations.get(0) : allPossibleLocations.get(getRandom().nextInt(allPossibleLocations.size() - 1));
        game.moveSphere(reserveSphere, location);
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
     * @param fullSquareLocation
     * @return sphere on board that can be moved to location or null
     */
    private PylosSphere getMovableSphereToLocation(PylosLocation fullSquareLocation) {
        for (PylosSphere ps : currentBoard.getSpheres(this)) {
            if (!ps.isReserve() && ps.canMoveTo(fullSquareLocation))
                return ps;
        }
        return null;
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        // Update current game & board
        currentGame = game;
        currentBoard = board;

        // removeSphere a random sphere - temporary solution
        List<PylosSphere> removableSpheres = new ArrayList<>();
        for (PylosSphere ps : board.getSpheres(this)) {
            if (!ps.isReserve() && !ps.getLocation().hasAbove()) {
                removableSpheres.add(ps);
            }
        }
        Collections.shuffle(removableSpheres);
        PylosSphere sphereToRemove;
        if (!removableSpheres.isEmpty()) {
            sphereToRemove = removableSpheres.get(0);
            game.removeSphere(sphereToRemove);
        } else {
            game.pass();
        }
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        //temporary solution
        doRemove(game, board);
    }
}
