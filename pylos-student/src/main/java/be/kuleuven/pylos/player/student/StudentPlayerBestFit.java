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

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        // Get all usable locations
        final List<PylosLocation> allPossibleLocations = new ArrayList<>();
        for (PylosLocation bl : board.getLocations()) {
            if (bl.isUsable()) {
                allPossibleLocations.add(bl);
            }
        }
        // Collections.shuffle(allPossibleLocations);

        // Make square own colours if possible
        final PylosLocation fullSquareLocation = getSquareLocation(allPossibleLocations);
        if (fullSquareLocation != null) {
            // Try to move sphere that is already on board
            PylosSphere sphere = null;
            for (PylosSphere ps : board.getSpheres(this)) {
                if (!ps.isReserve() && ps.canMoveTo(fullSquareLocation))
                    sphere = ps;
            }
            // Use reserve sphere
            if (sphere == null)
                sphere = board.getReserve(this);
            game.moveSphere(sphere, fullSquareLocation);
            return;
        }

        //else random move temporary
        PylosSphere reserveSphere = board.getReserve(this);
        PylosLocation location = allPossibleLocations.size() == 1 ? allPossibleLocations.get(0) : allPossibleLocations.get(getRandom().nextInt(allPossibleLocations.size() - 1));
        game.moveSphere(reserveSphere, location);
    }

    /**
     * @param allPossibleLocations
     * @return first location that forms square own colour
     */
    private PylosLocation getSquareLocation(List<PylosLocation> allPossibleLocations) {
        for (PylosLocation pylosLocation : allPossibleLocations) {
            for (PylosSquare pylosSquare : pylosLocation.getSquares()) {
                if (pylosSquare.getInSquare(this) == 3) {
                    return pylosLocation;
                }
            }
        }
        return null;
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
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
