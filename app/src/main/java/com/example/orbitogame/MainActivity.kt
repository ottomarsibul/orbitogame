package com.example.orbitogame

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.orbitogame.R
import kotlin.math.abs

enum class Stone { EMPTY, BLACK, WHITE }

class MainActivity : ComponentActivity() {
    private val highlightedSquares = mutableListOf<Pair<Int, Int>>()
    private var boardState = Array(4) { Array(4) { Stone.EMPTY } }
    private var currentPlayer = Stone.BLACK
    private var selectedStone: Pair<Int, Int>? = null
    private lateinit var boardButtons: Array<Array<Button>>
    private lateinit var playerTurnText: TextView
    private lateinit var actionPhaseText: TextView
    private var gamePhase = "Drop"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerTurnText = findViewById(R.id.playerTurnText)
        actionPhaseText = findViewById(R.id.actionPhaseText)
        val rotateButton: Button = findViewById(R.id.rotateButton)
        val newGameButton: Button = findViewById(R.id.newGameButton)
        val boardGrid: GridLayout = findViewById(R.id.boardGrid)

        boardButtons = Array(4) { row ->
            Array(4) { col ->
                Button(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 200
                        height = 200
                        rowSpec = GridLayout.spec(row)
                        columnSpec = GridLayout.spec(col)
                    }
                    text = ""
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5F5DC"))
                    boardGrid.addView(this)
                }
            }
        }

        for (row in 0 until 4) {
            for (col in 0 until 4) {
                val button = boardButtons[row][col]
                button.setOnClickListener { handleButtonClick(row, col) }
            }
        }

        rotateButton.setOnClickListener { if (gamePhase == "Rotate") rotateBoard() }
        newGameButton.setOnClickListener { resetGame() }
    }

    private fun resetGame() {
        for (row in 0..3) {
            for (col in 0..3) {
                boardButtons[row][col].text = ""
            }
        }
        currentPlayer = Stone.BLACK
        gamePhase = "Drop"
        selectedStone = null
        updateUI()
    }

    private fun updateUI() {
        playerTurnText.text = "${if (currentPlayer == Stone.BLACK) "Black" else "White"} To move"
        actionPhaseText.text = "Action: $gamePhase"
        updateBoardUI()
    }

    private fun updateBoardUI() {
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                val button = boardButtons[row][col]
                button.text = when (boardState[row][col]) {
                    Stone.BLACK -> "⚫"
                    Stone.WHITE -> "⚪"
                    Stone.EMPTY -> ""
                }
            }
        }
    }

    private fun getValidMoves(row: Int, col: Int): List<Pair<Int, Int>> {
        val directions = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
        return directions.mapNotNull { (dr, dc) ->
            val nr = row + dr
            val nc = col + dc
            if (nr in 0..3 && nc in 0..3 && boardState[nr][nc] == Stone.EMPTY) Pair(nr, nc) else null
        }
    }

    private fun handleButtonClick(row: Int, col: Int) {
        when (gamePhase) {
            "Move" -> {
                if (selectedStone == null) {
                    if (boardState[row][col] != currentPlayer && boardState[row][col] != Stone.EMPTY) {
                        selectedStone = Pair(row, col)
                        highlightValidMoves(row, col)
                    }
                } else {
                    if (boardState[row][col] == Stone.EMPTY && isAdjacent(row, col, selectedStone!!)) {
                        moveStone(selectedStone!!, row, col)
                        selectedStone = null
                        clearHighlights()
                        gamePhase = "Drop"
                        updateUI()
                    }
                }
            }
            "Drop" -> {
                if (boardState[row][col] == Stone.EMPTY) {
                    boardState[row][col] = currentPlayer
                    gamePhase = "Rotate"
                    updateUI()
                }
            }
        }
    }

    private fun highlightValidMoves(row: Int, col: Int) {
        val validMoves = getValidMoves(row, col)
        for ((mr, mc) in validMoves) {
            boardButtons[mr][mc].backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCE4C9"))
            highlightedSquares.add(Pair(mr, mc))
        }
    }

    private fun clearHighlights() {
        for ((row, col) in highlightedSquares) {
            boardButtons[row][col].backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5F5DC"))
        }
        highlightedSquares.clear()
    }

    private fun isAdjacent(row: Int, col: Int, selected: Pair<Int, Int>): Boolean {
        val (sr, sc) = selected
        return (abs(sr - row) + abs(sc - col)) == 1
    }

    private fun moveStone(from: Pair<Int, Int>, toRow: Int, toCol: Int) {
        val (fr, fc) = from
        boardState[toRow][toCol] = boardState[fr][fc]
        boardState[fr][fc] = Stone.EMPTY
        updateBoardUI()
    }

    private fun rotateBoard() {
        val outer = listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0), Pair(3, 1), Pair(3, 2), Pair(3, 3), Pair(2, 3), Pair(1, 3), Pair(0, 3), Pair(0, 2), Pair(0, 1))
        val inner = listOf(Pair(1, 1), Pair(2, 1), Pair(2, 2), Pair(1, 2))
        rotateSquare(outer)
        rotateSquare(inner)
        updateBoardUI()
        if (!checkForWin()) checkForDraw()
        gamePhase = "Move"
        changePlayer()
        updateUI()
    }

    private fun rotateSquare(square: List<Pair<Int, Int>>) {
        val temp = boardState[square.last().first][square.last().second]
        for (i in square.size - 1 downTo 1) {
            val (pr, pc) = square[i - 1]
            val (cr, cc) = square[i]
            boardState[cr][cc] = boardState[pr][pc]
        }
        val (fr, fc) = square[0]
        boardState[fr][fc] = temp
    }

    private fun changePlayer() {
        currentPlayer = if (currentPlayer == Stone.BLACK) Stone.WHITE else Stone.BLACK
    }

    private fun checkForWin(): Boolean {
        for (r in 0..3) {
            if (boardState[r][0] != Stone.EMPTY && boardState[r][0] == boardState[r][1] && boardState[r][1] == boardState[r][2] && boardState[r][2] == boardState[r][3]) {
                announceWinner()
                return true
            }
        }
        for (c in 0..3) {
            if (boardState[0][c] != Stone.EMPTY && boardState[0][c] == boardState[1][c] && boardState[1][c] == boardState[2][c] && boardState[2][c] == boardState[3][c]) {
                announceWinner()
                return true
            }
        }
        if (boardState[0][0] != Stone.EMPTY && boardState[0][0] == boardState[1][1] && boardState[1][1] == boardState[2][2] && boardState[2][2] == boardState[3][3]) {
            announceWinner()
            return true
        }
        if (boardState[0][3] != Stone.EMPTY && boardState[0][3] == boardState[1][2] && boardState[1][2] == boardState[2][1] && boardState[2][1] == boardState[3][0]) {
            announceWinner()
            return true
        }
        return false
    }

    private fun announceWinner() {
        Toast.makeText(this, "$currentPlayer wins!", Toast.LENGTH_LONG).show()
    }

    private fun checkForDraw(): Boolean {
        for (row in 0..3) {
            for (col in 0..3) {
                if (boardState[row][col] == Stone.EMPTY) return false
            }
        }
        Toast.makeText(this, "It's a draw!", Toast.LENGTH_LONG).show()
        return true
    }
}
