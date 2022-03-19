package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// TODO: When you remove the 4th sphere of your own square and make the square again, you con
//  remove 2 spheres again

public class StudentPlayerBestFit extends PylosPlayer {
  private final int NUMBER_OF_SPHERES = 15;
  private final int DEPTH = 2;
  private PylosGameIF currentGame;
  private PylosBoard currentBoard;
  private PylosGameSimulator simulator;
  private int currentDepth = 0;

  @Override
  public void doMove(PylosGameIF game, PylosBoard board) {
    currentGame = game;
    currentBoard = board;
    simulator = new PylosGameSimulator(currentGame.getState(), PLAYER_COLOR, currentBoard);
    final List<PylosLocation> allPossibleLocations = getAllPossibleLocations();
    Collections.shuffle(allPossibleLocations);
    if (makeSquare(this, allPossibleLocations)) return;
    else if (makeSquare(this.OTHER, allPossibleLocations)) return;
    miniMax();
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
      simulator.undoRemoveFirstSphere(sphereToRemove, prevLocation, PylosGameState.REMOVE_FIRST,
          PLAYER_COLOR);
    }
    game.removeSphere(bestSphereToRemove);
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
      simulator.undoRemoveSecondSphere(sphereToRemove, prevLocation, PylosGameState.REMOVE_SECOND,
          PLAYER_COLOR);
    }
    simulator.pass();
    double bestScorePass = miniMaxRec();
    simulator.undoPass(PylosGameState.REMOVE_SECOND, PLAYER_COLOR);
    if (bestScorePass > bestScoreRemove || bestSphereToRemove == null) game.pass();
    else game.removeSphere(bestSphereToRemove);
  }

  /**
   * @return score of the best possible move by current player simulator
   */
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
        if (maximizingPlayer && nextScore > bestScore ||
            !maximizingPlayer && nextScore < bestScore) {
          bestScore = nextScore;
        }
        if (prevLocation == null) simulator.undoAddSphere(sphere, prevState, prevColor);
        else simulator.undoMoveSphere(sphere, prevLocation, prevState, prevColor);
      }
    }
    return bestScore;
  }

  /**
   * @return score of the best possible remove by current player simulator
   */
  public double doRemoveSim() {
    PylosPlayerColor simulatorColor = simulator.getColor();
    PylosPlayer simulatorPlayer = (simulatorColor == PLAYER_COLOR) ? this : OTHER;
    final List<PylosSphere> removableSpheres = getRemovableSpheres(simulatorPlayer);
    double bestScore = (simulatorColor == PLAYER_COLOR) ? -9999 : 9999;
    for (PylosSphere sphereToRemove : removableSpheres) {
      PylosLocation prevLocation = sphereToRemove.getLocation();
      simulator.removeSphere(sphereToRemove);
      double result = miniMaxRec();
      if (simulatorColor == PLAYER_COLOR && result > bestScore) bestScore = result;
      else if (simulatorColor != PLAYER_COLOR && result < bestScore) bestScore = result;
      simulator.undoRemoveFirstSphere(sphereToRemove, prevLocation, PylosGameState.REMOVE_FIRST,
          simulatorColor);
    }
    return bestScore;
  }

  /**
   * @return score of the best remove or current board by current player simulator
   */
  private double doRemoveOrPassSim() {
    PylosPlayerColor simulatorColor = simulator.getColor();
    PylosPlayer simulatorPlayer = (simulatorColor == PLAYER_COLOR) ? this : OTHER;
    final List<PylosSphere> removableSpheres = getRemovableSpheres(simulatorPlayer);
    double bestScore = (simulatorColor == PLAYER_COLOR) ? -9999 : 9999;

    for (PylosSphere sphereToRemove : removableSpheres) {
      PylosLocation prevLocation = sphereToRemove.getLocation();
      simulator.removeSphere(sphereToRemove);
      double result = miniMaxRec();
      if (simulatorColor == PLAYER_COLOR && result > bestScore) bestScore = result;
      else if (simulatorColor != PLAYER_COLOR && result < bestScore) bestScore = result;
      simulator.undoRemoveSecondSphere(sphereToRemove, prevLocation, PylosGameState.REMOVE_SECOND,
          simulatorColor);
    }
    simulator.pass();
    double result = miniMaxRec();
    if (simulatorColor == PLAYER_COLOR && result > bestScore) bestScore = result;
    else if (simulatorColor != PLAYER_COLOR && result < bestScore) bestScore = result;
    simulator.undoPass(PylosGameState.REMOVE_SECOND, simulatorColor);
    return bestScore;
  }

  /**
   * Do best possible move calculated by recMiniMax
   */
  public void miniMax() {
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
    if (currentDepth == DEPTH) return evaluateBoard();
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
        score = evaluateBoard();
        break;
      default:
        throw new IllegalStateException("Unsupported game state: " + prevState);
    }
    currentDepth--;
    return score;
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
      // Get best full square location by minimax
      PylosLocation bestFullSquareLocation = null;
      PylosSphere bestSphere = null;
      double bestScore = -9999;
      for (PylosLocation fullSquareLocation : fullSquareLocations) {
        List<PylosSphere> movableSpheresToLocation =
            getMovableSpheresToLocation(fullSquareLocation);
        movableSpheresToLocation.add(currentBoard.getReserve(this)); // add reserve sphere
        for (PylosSphere sphere : movableSpheresToLocation) {
          PylosLocation prevLocation = sphere.getLocation();
          simulator.moveSphere(sphere, fullSquareLocation);
          double result = miniMaxRec();
          if (prevLocation == null)
            simulator.undoAddSphere(sphere, PylosGameState.MOVE, PLAYER_COLOR);
          else simulator.undoMoveSphere(sphere, prevLocation, PylosGameState.MOVE, PLAYER_COLOR);
          if (result > bestScore) {
            bestScore = result;
            bestFullSquareLocation = fullSquareLocation;
            bestSphere = sphere;
          }
        }
      }
      currentGame.moveSphere(bestSphere, bestFullSquareLocation);
      return true;
    }
    return false;
  }

  // Emphasizes preserving the spheres of our player and making full squares of our spheres
  private double evaluateBoard() {
    double reserveScore = (-30 * (NUMBER_OF_SPHERES - currentBoard.getReservesSize(this))) +
        (10 * (NUMBER_OF_SPHERES - currentBoard.getReservesSize(this.OTHER)));
    double moveScore = 0;
    List<PylosLocation> allPossibleLocations = getAllPossibleLocations();
    moveScore += (10 * getMakeSquaresSimulator(this, allPossibleLocations));
    moveScore += (doLevelUpSimulator(allPossibleLocations));
    moveScore += (10 * (getBlockedSpheres(OTHER) - getBlockedSpheres(this)));
    return reserveScore + moveScore;
  }

  /**
   * @param player
   * @param allPossibleLocations
   * @return successfully made/blocked square or not
   */
  private double getMakeSquaresSimulator(PylosPlayer player,
                                         List<PylosLocation> allPossibleLocations) {
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

  // Returns nr of spheres on board which can not be moved (they're blocking squares or have
  // spheres on top of them)
  private int getBlockedSpheres(PylosPlayer player) {
    int blocked = 0;
    for (PylosSphere sphere : currentBoard.getSpheres(player)) {
      if (sphere.getLocation() != null) {
        if (!sphere.canMove()) blocked++;
        else {
          for (PylosSquare square : sphere.getLocation().getSquares()) {
            if (square.getInSquare(player.OTHER) == 3) {
              blocked++;
              break;
            }
          }
        }
      }
    }
    return blocked;
  }


  /**
   * @param removableSpheres
   * @return sphere that doesn't give opponent opportunity to make square and doesn't break our
   * opportunity to create square
   * @deprecated
   */
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

  /**
   * @param location
   * @return sphere on board that can be moved to location or null
   */
  private List<PylosSphere> getMovableSpheresToLocation(PylosLocation location) {
    List<PylosSphere> movableSpheresToLocation = new ArrayList<>();
    for (PylosSphere ps : currentBoard.getSpheres(this)) {
      if (!ps.isReserve() && ps.canMoveTo(location))
        movableSpheresToLocation.add(ps);
    }
    return movableSpheresToLocation;
  }

  /**
   * @param sphere
   * @return list of locations where sphere can be placed,
   * spheres already on the board can not be placed on the square they currently occupy
   */
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
}
