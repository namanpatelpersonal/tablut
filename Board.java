package tablut;

import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import static tablut.Move.ROOK_MOVES;
import static tablut.Piece.*;
import static tablut.Square.*;
import static tablut.Utils.error;


/** The state of a Tablut Game.
 *  @author Naman Patel
 */
class Board {

    /** The number of squares on a side of the board. */
    static final int SIZE = 9;

    /** The throne (or castle) square and its four surrounding squares.. */
    static final Square THRONE = sq(4, 4),
        NTHRONE = sq(4, 5),
        STHRONE = sq(4, 3),
        WTHRONE = sq(3, 4),
        ETHRONE = sq(5, 4);

    /** Initial positions of attackers. */
    static final Square[] INITIAL_ATTACKERS = {
        sq(0, 3), sq(0, 4), sq(0, 5), sq(1, 4),
        sq(8, 3), sq(8, 4), sq(8, 5), sq(7, 4),
        sq(3, 0), sq(4, 0), sq(5, 0), sq(4, 1),
        sq(3, 8), sq(4, 8), sq(5, 8), sq(4, 7)
    };

    /** Initial positions of defenders of the king. */
    static final Square[] INITIAL_DEFENDERS = {
        NTHRONE, ETHRONE, STHRONE, WTHRONE,
        sq(4, 6), sq(4, 2), sq(2, 4), sq(6, 4)
    };

    /** Initializes a game board with SIZE squares on a side in the
     *  initial position. */
    Board() {
        init();
    }

    /** Initializes a copy of MODEL. */
    Board(Board model) {
        copy(model);
    }

    /** Copies MODEL into me. */
    void copy(Board model) {
        if (model == this) {
            return;
        }
        init();
        this._winner = model._winner;
        this._turn = model._turn;
        this._moveCount = model._moveCount;
        this._repeated = model._repeated;
        this.myPieces = model.myPieces;
        this.maxMoves = model.maxMoves;
        this.checkMoveLimit = model.checkMoveLimit;
        this.squareStack = model.squareStack;
        this.pieceStack = model.pieceStack;
        this.capturedPieceStack = model.capturedPieceStack;
        this.boardPositionsStack = model.boardPositionsStack;
    }

    /** Clears the board to the initial position. */
    void init() {
        _winner = null;
        _moveCount = 0;
        squareStack.clear();
        pieceStack.clear();
        capturedPieceStack.clear();
        boardPositionsStack.clear();
        for (int i = 0;  i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                myPieces[i][j] = EMPTY;
            }
        }
        for (Square sq: INITIAL_ATTACKERS) {
            int colIndex = sq.col();
            int rowIndex = sq.row();
            myPieces[colIndex][rowIndex] = BLACK;
        }
        for (Square sq: INITIAL_DEFENDERS) {
            int colIndex = sq.col();
            int rowIndex = sq.row();
            myPieces[colIndex][rowIndex] = WHITE;
        }
        myPieces[THRONE.col()][THRONE.row()] = KING;
        _turn = BLACK;
        _repeated = false;
        boardPositionsStack.push(encodedBoard());
        checkMoveLimit = false;
    }

    /** Set the move limit to LIM.  It is an error if 2*LIM <= moveCount().
     * @param n integer you want to set limit to. */
    void setMoveLimit(int n) {
        if (2 * n <= moveCount()) {
            checkMoveLimit = false;
            throw error("Illegal value for limit command");
        } else {
            checkMoveLimit = true;
            maxMoves = n;
        }
    }

    /** Return a Piece representing whose move it is (WHITE or BLACK). */
    Piece turn() {
        return _turn;
    }

    /** Return the winner in the current position, or null if there is no winner
     *  yet. */
    Piece winner() {
        return _winner;
    }

    /** Returns true iff this is a win due to a repeated position. */
    boolean repeatedPosition() {
        return _repeated;
    }

    /** Record current position and set winner() next mover if the current
     *  position is a repeat. */
    private void checkRepeated() {
        if (boardPositionsStack.contains(encodedBoard())) {
            _repeated = true;
            _winner = turn().opponent();
        } else {
            boardPositionsStack.push(encodedBoard());
        }
    }

    /** Return the number of moves since the initial position that have not been
     *  undone. */
    int moveCount() {
        return _moveCount;
    }

    /** Return location of the king. */
    Square kingPosition() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (myPieces[i][j] == KING) {
                    return Square.sq(j * SIZE + i);
                }
            }
        }
        throw error("No King found");
    }

    /** Return the contents the square at S. */
    final Piece get(Square s) {
        return get(s.col(), s.row());
    }

    /** Return the contents of the square at (COL, ROW), where
     *  0 <= COL, ROW <= 9. */
    final Piece get(int col, int row) {
        return myPieces[col][row];
    }

    /** Return the contents of the square at COL ROW. */
    final Piece get(char col, char row) {
        return get(row - '1', col - 'a');
    }

    /** Set square S to P. */
    final void put(Piece p, Square s) {
        myPieces[s.col()][s.row()] = p;
    }

    /** Set square S to P and record for undoing. */
    final void revPut(Piece p, Square s) {
        put(p, s);
        pieceStack.push(p);
        squareStack.push(s);
    }

    /** Set square COL ROW to P. */
    final void put(Piece p, char col, char row) {
        put(p, sq(col - 'a', row - '1'));
    }

    /** Return true iff FROM - TO is an unblocked rook move on the current
     *  board.  For this to be true, FROM-TO must be a rook move and the
     *  squares along it, other than FROM, must be empty. */
    boolean isUnblockedMove(Square from, Square to) {
        if (from.isRookMove(to)) {
            if (exists(to.col(), to.row())) {
                int targetDirection = from.direction(to);
                for (Square sq : ROOK_SQUARES[from.index()][targetDirection]) {
                    if (get(sq) != EMPTY) {
                        return false;
                    }
                    if (sq == to) {
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }


    /** Return true iff FROM is a valid starting square for a move. */
    boolean isLegal(Square from) {
        return get(from).side() == _turn;
    }

    /** Return true iff FROM-TO is a valid move. */

    boolean isLegal(Square from, Square to) {
        if (!isLegal(from)) {
            return false;
        }
        if (!exists(to.col(), to.row()) || get(to.col(), to.row()) != EMPTY) {
            return false;
        }
        if (!isUnblockedMove(from, to)) {
            return false;
        }
        if (to == THRONE && get(from.col(), from.row()) != KING) {
            return false;
        }
        return true;
    }

    /** Return true iff MOVE is a legal move in the current
     *  position. */
    boolean isLegal(Move move) {
        return isLegal(move.from(), move.to());
    }

    /** Move FROM-TO, assuming this is a legal move. */
    void makeMove(Square from, Square to) {
        int moveCaptureCount = 0;
        assert isLegal(from, to);
        if (((_moveCount / 2) >= maxMoves) && (checkMoveLimit)) {
            _winner = turn().opponent();
            return;
        }
        Piece movingPiece = myPieces[from.col()][from.row()];
        revPut(movingPiece, to);
        revPut(EMPTY, from);
        checkRepeated();
        for (int i = 0; i <= 3; i++) {
            if (capture(to, to.rookMove(i, 2))) {
                moveCaptureCount++;
            }
        }
        numberCapturedStack.push(moveCaptureCount);
        if (_winner == null) {
            if (kingPosition().isEdge()) {
                _winner = WHITE;
            }
        }
        _moveCount++;
        _turn = turn().opponent();
    }

    /** Move according to MOVE, assuming it is a legal move. */
    void makeMove(Move move) {
        makeMove(move.from(), move.to());
    }

    /** Capture the piece between SQ0 and SQ2, assuming a piece just moved to
     *  SQ0 and the necessary conditions are satisfied.
     *  @return boolean true if piece was captured*/
    private boolean capture(Square sq0, Square sq2) {
        boolean captured = false;
        if (sq2 == null || !exists(sq2.col(), sq2.row())) {
            return captured;
        }
        Piece movingPiece = get(sq0);
        Piece stationaryPiece = get(sq2);
        Square middle = sq0.between(sq2);
        Piece middlePiece = get(middle);
        if (middlePiece == BLACK) {
            if ((movingPiece.side() == WHITE && stationaryPiece.side() == WHITE)
                    || (movingPiece.side() == WHITE && sq2 == THRONE)) {
                capturedPieceStack.push(middlePiece);
                revPut(EMPTY, middle);
                captured = true;
            }
        } else if (middlePiece == WHITE) {
            if (movingPiece.side() == BLACK
                    && stationaryPiece.side() == BLACK) {
                capturedPieceStack.push(middlePiece);
                revPut(EMPTY, middle);
                captured = true;
            } else if (movingPiece.side() == BLACK && sq2 == THRONE) {
                if (stationaryPiece == EMPTY) {
                    capturedPieceStack.push(middlePiece);
                    revPut(EMPTY, middle);
                    captured = true;
                } else if (stationaryPiece == KING) {
                    if (hostileOccupiedThrone()) {
                        capturedPieceStack.push(middlePiece);
                        revPut(EMPTY, middle);
                        captured = true;
                    }
                }
            }
        } else if (middlePiece == KING) {
            if (middle == THRONE || middle == NTHRONE || middle == ETHRONE
                    || middle == STHRONE || middle == WTHRONE) {
                if (surroundedKing(middle, sq0, sq2)) {
                    capturedPieceStack.push(middlePiece);
                    revPut(EMPTY, middle);
                    captured = true;
                    _winner = BLACK;
                }
            } else {
                if (movingPiece.side() == BLACK
                        && stationaryPiece.side() == BLACK) {
                    capturedPieceStack.push(middlePiece);
                    revPut(EMPTY, middle);
                    captured = true;
                    _winner = BLACK;
                }
            }
        }
        return captured;
    }

    /** Boolean return function that checks whether the
     * occupied throne surrounded by black pieces on 3 sides
     * and thus its hostility to a white piece. */
    private boolean hostileOccupiedThrone() {
        Square[] kingdom = new Square[] {NTHRONE, ETHRONE, STHRONE, WTHRONE};
        int numBlack = 0;
        for (Square square : kingdom) {
            if (get(square) == BLACK) {
                numBlack++;
            }
        }
        return numBlack == 3;
    }

    /** Boolean return function that checks whether the
     * king on its throne and four adjacent squares is surrounded by
     * hostile squares on four sides.
     * @param middleSquare the middle square
     * @param  sq0 the square of moving piece
     * @param  sq2 the square of stationary piece. */
    private boolean surroundedKing(Square middleSquare,
                                   Square sq0, Square sq2) {
        int numBlack = 0;
        if (middleSquare == THRONE) {
            Square[] kingdom =
                    new Square[] {NTHRONE, ETHRONE, STHRONE, WTHRONE};
            for (Square square : kingdom) {
                if (get(square) == BLACK) {
                    numBlack++;
                }
            }
            return numBlack == 4;
        } else {
            Square[] fourSquares = new Square[] {sq0, sq2,
                    sq0.diag1(sq2), sq0.diag2(sq2)};
            for (Square square : fourSquares) {
                if (get(square) == BLACK) {
                    numBlack++;
                }
            }
            return numBlack == 3;
        }
    }


    /** Undo one move.  Has no effect on the initial board. */
    void undo() {
        _winner = null;
        if (_moveCount > 0) {
            undoPosition();
            int lastMoveCaptureCount = numberCapturedStack.pop();
            switch (lastMoveCaptureCount) {
            case 1:
                Piece pieceCaptured1 = capturedPieceStack.pop();
                Square middleSquare1 = squareStack.pop();
                pieceStack.pop();
                put(pieceCaptured1, middleSquare1);
                break;
            case 2:
                Piece pieceCaptured2 = capturedPieceStack.pop();
                Square middleSquare2 = squareStack.pop();
                pieceStack.pop();
                put(pieceCaptured2, middleSquare2);
                Piece pieceCaptured3 = capturedPieceStack.pop();
                Square middleSquare3 = squareStack.pop();
                pieceStack.pop();
                put(pieceCaptured3, middleSquare3);
                break;
            case 3:
                Piece pieceCaptured4 = capturedPieceStack.pop();
                Square middleSquare4 = squareStack.pop();
                pieceStack.pop();
                put(pieceCaptured4, middleSquare4);
                Piece pieceCaptured5 = capturedPieceStack.pop();
                Square middleSquare5 = squareStack.pop();
                pieceStack.pop();
                put(pieceCaptured5, middleSquare5);
                Piece pieceCaptured6 = capturedPieceStack.pop();
                Square middleSquare6 = squareStack.pop();
                pieceStack.pop();
                put(pieceCaptured6, middleSquare6);
                break;
            default:
                break;
            }
            Square fromSquare = squareStack.pop();
            Piece fromPiece = pieceStack.pop();
            Square toSquare = squareStack.pop();
            Piece toPiece = pieceStack.pop();
            put(toPiece, fromSquare);
            put(fromPiece, toSquare);
            _moveCount--;
        } else {
            throw error("Initial board with 0 moves done");
        }
    }

    /** Remove record of current position in the set of positions encountered,
     *  unless it is a repeated position or we are at the first move. */
    private void undoPosition() {
        if (!(_repeated || moveCount() < 1)) {
            boardPositionsStack.pop();
        }
        if (_repeated) {
            _repeated = false;
        }
    }

    /** Clear the undo stack and board-position counts. Does not modify the
     *  current position or win status. */
    void clearUndo() {
        boardPositionsStack.clear();
        pieceStack.clear();
        squareStack.clear();
    }

    /** Return a new mutable list of all legal moves on the current board for
     *  SIDE (ignoring whose turn it is at the moment). */
    List<Move> legalMoves(Piece side) {
        List<Move> legalMoves = new Move.MoveList();
        for (Square s : pieceLocations(side)) {
            for (int x = 0; x < 4; x++) {
                for (Move move : ROOK_MOVES[s.index()][x]) {
                    if (isLegal(move)) {
                        legalMoves.add(move);
                    }
                }
            }
        }
        return legalMoves;
    }

    /** Return true iff SIDE has a legal move. */
    boolean hasMove(Piece side) {
        if (legalMoves(side).isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /** Return a text representation of this Board.  If COORDINATES, then row
     *  and column designations are included along the left and bottom sides.
     */
    String toString(boolean coordinates) {
        Formatter out = new Formatter();
        for (int r = SIZE - 1; r >= 0; r -= 1) {
            if (coordinates) {
                out.format("%2d", r + 1);
            } else {
                out.format("  ");
            }
            for (int c = 0; c < SIZE; c += 1) {
                out.format(" %s", get(c, r));
            }
            out.format("%n");
        }
        if (coordinates) {
            out.format("  ");
            for (char c = 'a'; c <= 'i'; c += 1) {
                out.format(" %c", c);
            }
            out.format("%n");
        }
        return out.toString();
    }

    /** Return the locations of all pieces on SIDE. */
    private HashSet<Square> pieceLocations(Piece side) {
        assert side != EMPTY;
        HashSet<Square> pieceLocations = new HashSet<>();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; i < SIZE; j++) {
                if (myPieces[i][j].side() == side) {
                    Square locationSq = sq(i, j);
                    pieceLocations.add(locationSq);
                }
            }
        }
        return pieceLocations;
    }

    /** Return the contents of _board in the order of SQUARE_LIST as a sequence
     *  of characters: the toString values of the current turn and Pieces. */
    String encodedBoard() {
        char[] result = new char[Square.SQUARE_LIST.size() + 1];
        result[0] = turn().toString().charAt(0);
        for (Square sq : SQUARE_LIST) {
            result[sq.index() + 1] = get(sq).toString().charAt(0);
        }
        return new String(result);
    }

    /** Piece whose turn it is (WHITE or BLACK). */
    private Piece _turn;
    /** Cached value of winner on this board, or EMPTY if it has not been
     *  computed. */
    private Piece _winner;
    /** Number of (still undone) moves since initial position. */
    private int _moveCount;
    /** True when current board is a repeated position (ending the game). */
    private boolean _repeated;
    /** Two dimensional array representing the pieces on the board. */
    private Piece[][] myPieces = new Piece[SIZE][SIZE];
    /** LIFO Stack that records the previous board positions. */
    private Stack<String> boardPositionsStack = new Stack<>();
    /** LIFO Stack that records the pieces. */
    private Stack<Piece> pieceStack = new Stack<>();
    /** LIFO Stack that records the squares. */
    private Stack<Square> squareStack = new Stack<>();
    /** LIFO Stack that records the captured pieces. */
    private Stack<Piece> capturedPieceStack = new Stack<>();
    /** LIFO Stack that records the number of captured pieces per move. */
    private Stack<Integer> numberCapturedStack = new Stack<>();
    /** Integer that represents the move limit of game. */
    private int maxMoves;
    /** Boolean that checks whether a move limit is enabled. */
    private boolean checkMoveLimit = false;
}
