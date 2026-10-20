package com.fourierart.app.core

import org.junit.Assert.*
import org.junit.Test

class DrawingTimelineTest {
    private fun drawing()=ContourDrawing.fromStrokes(listOf(
        ContourStroke(listOf(Point2(0.0,0.0),Point2(10.0,0.0)),false),
        ContourStroke(listOf(Point2(40.0,0.0),Point2(40.0,30.0)),false)))
    @Test fun durationsFollowVisibleLengthAndIgnoreFourierComponentBudget() {
        val timeline=DrawingTimeline(drawing())
        assertEquals(.25,timeline.endOf(0),1e-12);assertEquals(.25,timeline.startOf(1),1e-12)
        assertEquals(.5,timeline.frame(.125).localProgress,1e-12)
        assertEquals(.5,timeline.frame(.625).localProgress,1e-12)
    }
    @Test fun exactBoundaryLiftsPenAndStartsNextIndependentStroke() {
        val drawing=drawing();val timeline=DrawingTimeline(drawing)
        val before=timeline.frame(timeline.endOf(0)-1e-9);val at=timeline.frame(timeline.endOf(0))
        assertEquals(0,before.active);assertEquals(1,at.active);assertEquals(1,at.completed);assertEquals(0.0,at.localProgress,0.0)
        val a=drawing.strokes[0].position(1.0,800);val b=drawing.strokes[1].position(0.0,800)
        assertTrue(kotlin.math.hypot(b.x-a.x,b.y-a.y)>.5)
    }
    @Test fun completeFrameHasNoActivePenOrReturnJourney() {
        val timeline=DrawingTimeline(drawing())
        assertEquals(DrawingTimeline.Frame(2,null,1.0),timeline.frame(1.0))
        assertEquals(timeline.frame(1.0),timeline.frame(2.0))
        assertEquals(DrawingTimeline.Frame(0,0,0.0),timeline.frame(-1.0))
    }
    @Test fun frameMappingIsStatelessAndRepeatableOutOfOrder() {
        val a=DrawingTimeline(drawing());val b=DrawingTimeline(drawing())
        for(p in listOf(.7,0.0,.999,.25,.1,1.0,.7)) assertEquals(a.frame(p),b.frame(p))
    }
    @Test(expected=IllegalArgumentException::class) fun nonFiniteProgressIsRejected() {DrawingTimeline(drawing()).frame(Double.NaN)}
}
