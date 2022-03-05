package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
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
        List<PylosLocation> allPossibleLocations = new ArrayList<>();
        for (PylosLocation bl : board.getLocations()) {
            if (bl.isUsable()) {
                allPossibleLocations.add(bl);
            }
        }

        // Make square own colours if possible
        List<PylosLocation> squareLocations = getAllSquareLocations(allPossibleLocations);

    }

    /**
     * @param allPossibleLocations
     * @return all locations that form square own colour in random order
     */
    private List<PylosLocation> getAllSquareLocations(List<PylosLocation> allPossibleLocations) {
        List<PylosLocation> result = new ArrayList<>();
        for (PylosLocation pylosLocation : allPossibleLocations) {
            for (PylosSquare pylosSquare : pylosLocation.getSquares()) {
                if (pylosSquare.getInSquare(this) == 3) {
                    result.add(pylosLocation);
                    break;
                }
            }
        }
        Collections.shuffle(result);
        return result;
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        // removeSphere a random sphere - temporary solution
        ArrayList<PylosSphere> removableSpheres = new ArrayList<>();
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
