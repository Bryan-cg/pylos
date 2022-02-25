package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;

/**
 * Created by Ine on 5/05/2015.
 */
public class StudentPlayerRandomFit extends PylosPlayer {

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        int randomIndex = getRandom().nextInt(2);
        if (randomIndex == 0 && doLevelUp(game, board)) return;
        else doMoveReserveSphere(game, board);

    }

    private boolean doLevelUp(PylosGameIF game, PylosBoard board) {
        ArrayList<PylosSphere> removableSpheres = new ArrayList<>();
        for (PylosSphere ps : board.getSpheres(this)) {
            if (!ps.isReserve() && !ps.getLocation().hasAbove()) {
                removableSpheres.add(ps);
            }
        }
        Collections.shuffle(removableSpheres);
        PylosSphere boardSphere = null;
        if (!removableSpheres.isEmpty()) {
            boardSphere = removableSpheres.get(0);
        } else return false;
        PylosLocation oldLocation = boardSphere.getLocation();
        int z = boardSphere.getLocation().Z;
        ArrayList<PylosLocation> allPossibleTopLocations = new ArrayList<>();
        for (PylosLocation bl : board.getLocations()) {
            if (bl.isUsable() && !bl.hasAbove() && bl.Z > z) {
                allPossibleTopLocations.add(bl);
            }
        }

        List<PylosSquare> squares = oldLocation.getSquares();
        for (PylosSquare ps : squares) allPossibleTopLocations.remove(ps.getTopLocation());
        if (allPossibleTopLocations.isEmpty()) {
            return false;
        }
        Collections.shuffle(allPossibleTopLocations);
        game.moveSphere(boardSphere, allPossibleTopLocations.get(0));
        return true;
    }

    private void doMoveReserveSphere(PylosGameIF game, PylosBoard board) {
        /* add a reserve sphere to a feasible random location */
        ArrayList<PylosLocation> allPossibleLocations = new ArrayList<>();
        for (PylosLocation bl : board.getLocations()) {
            if (bl.isUsable()) {
                allPossibleLocations.add(bl);
            }
        }
        PylosSphere reserveSphere = board.getReserve(this);
        PylosLocation location = allPossibleLocations.size() == 1 ? allPossibleLocations.get(0) : allPossibleLocations.get(getRandom().nextInt(allPossibleLocations.size() - 1));
        game.moveSphere(reserveSphere, location);
    }


    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        /* removeSphere a random sphere */
        ArrayList<PylosSphere> removableSpheres = new ArrayList<>();
        for (PylosSphere ps : board.getSpheres(this)) {
            if (!ps.isReserve() && !ps.getLocation().hasAbove()) {
                removableSpheres.add(ps);
            }
        }
        Collections.shuffle(removableSpheres);
        PylosSphere sphereToRemove;
        if (!removableSpheres.isEmpty()){
            sphereToRemove = removableSpheres.get(0);
            game.removeSphere(sphereToRemove);
        } else {
            game.pass();
        }
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        // always remove two
        doRemove(game, board);
    }
}
