package com.fourierart.app.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.fourierart.app.R
import com.fourierart.app.photo.PhotoSessionModel

@Composable
fun HomeScreen(model: PhotoSessionModel,onChoose: ()->Unit,onCamera: ()->Unit,onResume: ()->Unit,onDemo: ()->Unit,onSample: (Boolean)->Unit,onGuide: ()->Unit,onSettings: ()->Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().testTag("screen_home")) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text("FOURIER",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.ExtraBold,modifier=Modifier.semantics{heading()})
            Text("ART",style=MaterialTheme.typography.displaySmall,color=Aqua,fontWeight=FontWeight.ExtraBold)
            Text(stringResource(R.string.tagline),color=Muted,style=MaterialTheme.typography.titleMedium)
            FourierEmblem(Modifier.fillMaxWidth().height(180.dp))
            if(model.notice!=0)StatusMessage(stringResource(model.notice),modifier=Modifier.testTag("photo_notice"))
            if(model.loading)StatusMessage(stringResource(R.string.preparing_photo),loading=true,modifier=Modifier.testTag("photo_loading"))
            ActionButton(stringResource(R.string.take_photo),onCamera,Modifier.fillMaxWidth().testTag("take_photo"),!model.loading,Glyph.CAMERA)
            OutlinedButton(onChoose,Modifier.fillMaxWidth().heightIn(min=52.dp).testTag("choose_photo"),enabled=!model.loading) {
                LineIcon(Glyph.PHOTO);Spacer(Modifier.width(10.dp));Text(stringResource(R.string.choose_photo))
            }
            Text(stringResource(R.string.home_hint),color=Muted,style=MaterialTheme.typography.bodySmall)
            if(model.source!=null) SectionCard(stringResource(R.string.current_work)) {
                Text(stringResource(if(model.editing)R.string.resume_preparation else R.string.resume_contours),color=Muted)
                ActionButton(stringResource(R.string.continue_work),onResume,Modifier.fillMaxWidth().testTag("resume_work"),!model.loading)
                Text(stringResource(R.string.working_photo_notice),color=Muted,style=MaterialTheme.typography.bodySmall)
            }
            SectionCard(stringResource(R.string.explore_demo)) {
                Text(stringResource(R.string.demo_hint),color=Muted,style=MaterialTheme.typography.bodySmall)
                if(model.source==null) ActionButton(stringResource(R.string.view_demo),onDemo,Modifier.fillMaxWidth().testTag("resume_animation"),!model.loading,Glyph.PLAY)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({onSample(true)},Modifier.weight(1f).testTag("sample_circle"),enabled=!model.loading){Text(stringResource(R.string.circle))}
                    OutlinedButton({onSample(false)},Modifier.weight(1f).testTag("sample_star"),enabled=!model.loading){Text(stringResource(R.string.star))}
                }
            }
            Text(stringResource(R.string.home_privacy),color=Muted,style=MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider(color=MaterialTheme.colorScheme.outline)
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
            TextButton(onGuide,Modifier.weight(1f).heightIn(min=52.dp).testTag("open_guide")){LineIcon(Glyph.GUIDE);Spacer(Modifier.width(8.dp));Text(stringResource(R.string.guide_title))}
            TextButton(onSettings,Modifier.weight(1f).heightIn(min=52.dp).testTag("open_settings")){LineIcon(Glyph.SETTINGS);Spacer(Modifier.width(8.dp));Text(stringResource(R.string.settings_title))}
        }
    }
}

@Composable
fun GuideScreen(onBack: ()->Unit) {
    WorkflowPage(stringResource(R.string.guide_title),onBack,tag="screen_guide") {
        Text(stringResource(R.string.guide_intro),color=Muted)
        for((title,description) in listOf(
            R.string.guide_photo_title to R.string.guide_photo_body,
            R.string.prepare_title to R.string.guide_prepare_body,
            R.string.review_title to R.string.guide_contour_body,
            R.string.animation_parameters to R.string.guide_parameters_body,
            R.string.animation_title to R.string.guide_animation_body,
            R.string.export_title to R.string.guide_export_body
        )) SectionCard(stringResource(title)) {Text(stringResource(description),style=MaterialTheme.typography.bodyMedium,color=Muted)}
        SectionCard(stringResource(R.string.photo_tips)) {Text(stringResource(R.string.photo_tips_body),color=Muted)}
        SectionCard(stringResource(R.string.local_model_title)) {Text(stringResource(R.string.local_model_body),color=Muted)}
    }
}

@Composable
fun SettingsScreen(model: PhotoSessionModel,onBack: ()->Unit,onDiscard: ()->Unit,onPrivacy: ()->Unit) {
    val language=LocalContext.current.resources.configuration.locales[0].language
    WorkflowPage(stringResource(R.string.settings_title),onBack,tag="screen_settings") {
        SectionCard(stringResource(R.string.language_label)) {
            ChoiceButton("Español",language=="es",{AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))},Modifier.fillMaxWidth().testTag("language_es"))
            ChoiceButton("English",language=="en",{AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))},Modifier.fillMaxWidth().testTag("language_en"))
            TextButton(onClick={AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())},modifier=Modifier.fillMaxWidth().testTag("language_system")) {Text(stringResource(R.string.language_system))}
        }
        SectionCard(stringResource(R.string.privacy_title)) {
            Text(stringResource(R.string.privacy_body),color=Muted)
            Text(stringResource(R.string.local_model_body),color=Muted,style=MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.sdk_privacy_summary),color=Muted,style=MaterialTheme.typography.bodySmall)
            TextButton(onPrivacy,modifier=Modifier.fillMaxWidth().testTag("open_privacy")) {Text(stringResource(R.string.privacy_policy_title))}
        }
        SectionCard(stringResource(R.string.current_work)) {
            Text(stringResource(if(model.source==null)R.string.no_saved_photo else R.string.saved_photo_help),color=Muted)
            if(model.source!=null)OutlinedButton(onDiscard,enabled=!model.loading,modifier=Modifier.fillMaxWidth().testTag("settings_discard")){Text(stringResource(R.string.discard_photo))}
        }
        SectionCard(stringResource(R.string.about_title)) {
            Text("FOURIER ART",fontWeight=FontWeight.Bold)
            Text(stringResource(R.string.app_version),color=Muted)
            Text(stringResource(R.string.about_body),color=Muted,style=MaterialTheme.typography.bodySmall)
        }
    }
}
