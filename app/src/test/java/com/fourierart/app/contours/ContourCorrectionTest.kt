package com.fourierart.app.contours

import com.fourierart.app.core.*
import org.junit.Assert.*
import org.junit.Test

class ContourCorrectionTest {
    private fun line(x0: Double,y0: Double,x1: Double,y1: Double)=ContourStroke(listOf(Point2(x0,y0),Point2(x1,y1)),false)
    private val square=ContourStroke(listOf(Point2(0.0,0.0),Point2(100.0,0.0),Point2(100.0,100.0),Point2(0.0,100.0)),true)
    @Test fun defaultLeavesAcceptedDetectionUnchanged() {
        val input=listOf(square,line(10.0,20.0,80.0,30.0))
        assertEquals(input,ContourCorrection.apply(input,listOf(square),100,100,ContourRecipe()))
    }
    @Test fun erasingMiddleSplitsLongSegmentWithoutBridge() {
        val out=ContourCorrection.erase(listOf(line(0.0,50.0,100.0,50.0)),listOf(Point2(50.0,50.0)),10.0)
        assertEquals(2,out.size);assertEquals(40.0,out[0].points.last().x,1e-6);assertEquals(60.0,out[1].points.first().x,1e-6)
        assertTrue(out.all {!it.closed});assertEquals(80.0,out.sumOf {it.length},1e-6)
    }
    @Test fun fastSweptEraserDoesNotMissBetweenTouchEvents() {
        val input=listOf(line(0.0,50.0,100.0,50.0))
        val out=ContourCorrection.erase(input,listOf(Point2(50.0,0.0),Point2(50.0,100.0)),5.0)
        assertEquals(2,out.size);assertEquals(45.0,out[0].points.last().x,1e-6);assertEquals(55.0,out[1].points.first().x,1e-6)
    }
    @Test fun erasedClosedContourBecomesOneOpenStrokeAcrossItsOldSeam() {
        val out=ContourCorrection.erase(listOf(square),listOf(Point2(50.0,0.0)),10.0)
        assertEquals(1,out.size);assertFalse(out[0].closed);assertEquals(380.0,out[0].length,1e-6)
        assertEquals(60.0,out[0].points.first().x,1e-6);assertEquals(40.0,out[0].points.last().x,1e-6)
        val drawing=ContourDrawing.fromStrokes(out);assertEquals(.5,drawing.strokes[0].visibleFraction,0.0)
    }
    @Test fun eraserAwayFromClosedStrokePreservesItExactly() {
        assertEquals(listOf(square),ContourCorrection.erase(listOf(square),listOf(Point2(50.0,50.0)),5.0))
        assertTrue(ContourCorrection.erase(listOf(square),listOf(Point2(50.0,50.0)),100.0).isEmpty())
    }
    @Test fun keepingRegionCutsCrossingSegmentsAndNeverAddsBoxEdges() {
        val out=ContourCorrection.keep(listOf(square,line(-10.0,50.0,110.0,50.0)),20.0,20.0,80.0,80.0)
        assertEquals(1,out.size);assertEquals(listOf(Point2(20.0,50.0),Point2(80.0,50.0)),out[0].points);assertFalse(out[0].closed)
    }
    @Test fun croppedClosedStrokeRetainsSeparateOpenFragments() {
        val out=ContourCorrection.keep(listOf(square),20.0,0.0,80.0,100.0)
        assertEquals(2,out.size);assertTrue(out.all {!it.closed});assertEquals(120.0,out.sumOf {it.length},1e-6)
    }
    @Test fun normalizedRecipeUsesSameImageCoordinatesForNonSquareRaster() {
        val recipe=ContourRecipe(edits=listOf(ContourEdit.Draw(listOf(Point2(.1,.2),Point2(.9,.8)))))
        val stroke=ContourCorrection.apply(emptyList(),emptyList(),200,100,recipe).single()
        assertEquals(listOf(Point2(20.0,20.0),Point2(180.0,80.0)),stroke.points);assertFalse(stroke.closed)
    }
    @Test fun cleanupAndSilhouetteDoNotRemoveDeliberateManualMarks() {
        val details=listOf(square,line(50.0,50.0,51.0,50.0))
        val recipe=ContourRecipe(cleanup=100,edits=listOf(ContourEdit.Draw(listOf(Point2(.5,.5),Point2(.51,.5)))))
        assertEquals(2,ContourCorrection.apply(details,listOf(square),100,100,recipe).size)
        assertEquals(listOf(square),ContourCorrection.apply(details,listOf(square),100,100,ContourRecipe(ContourPreset.SILHOUETTE)))
    }
    @Test fun historyBranchesAndResetRemainUndoable() {
        val first=ContourRecipe(cleanup=30);val second=ContourRecipe(ContourPreset.SILHOUETTE)
        val history=CorrectionHistory().change(first).change(second)
        assertEquals(first,history.undo().current);assertEquals(second,history.undo().redo().current)
        assertTrue(history.undo().change(ContourRecipe(cleanup=70)).future.isEmpty())
        assertEquals(second,history.change(ContourRecipe()).undo().current)
        var bounded=CorrectionHistory();repeat(30) {bounded=bounded.change(ContourRecipe(cleanup=it))};assertEquals(20,bounded.past.size)
    }
    @Test(expected=IllegalArgumentException::class) fun invalidRadiusCannotErase() {
        ContourCorrection.erase(listOf(square),listOf(Point2(0.0,0.0)),Double.NaN)
    }
}
