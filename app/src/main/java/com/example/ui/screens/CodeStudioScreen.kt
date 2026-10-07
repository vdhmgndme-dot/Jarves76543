package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.ProjectEntity
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisSuccess
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

enum class CodeEditorTab {
    PREVIEW,
    HTML,
    CSS,
    JAVASCRIPT,
    CONSOLE
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CodeStudioScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProject by viewModel.activeProject.collectAsState()
    val allProjects by viewModel.projects.collectAsState()

    var selectedTab by remember { mutableStateOf(CodeEditorTab.PREVIEW) }
    var htmlContent by remember { mutableStateOf("") }
    var cssContent by remember { mutableStateOf("") }
    var jsContent by remember { mutableStateOf("") }

    var projectDropdownExpanded by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableStateOf(0) }
    val consoleLogs = remember { mutableStateListOf<String>() }

    // Sync active project state
    LaunchedEffect(activeProject?.id) {
        activeProject?.let {
            htmlContent = it.htmlCode
            cssContent = it.cssCode
            jsContent = it.jsCode
            consoleLogs.clear()
            consoleLogs.add("Loaded project: ${it.name}")
        }
    }

    // Build complete HTML payload
    val fullHtml = remember(htmlContent, cssContent, jsContent, reloadTrigger) {
        """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style>
    $cssContent
  </style>
  <script>
    (function() {
      var oldLog = console.log;
      var oldErr = console.error;
      console.log = function() {
        var args = Array.prototype.slice.call(arguments);
        if (window.jarvisBridge) {
          window.jarvisBridge.postLog(args.join(' '));
        }
        oldLog.apply(console, arguments);
      };
      console.error = function() {
        var args = Array.prototype.slice.call(arguments);
        if (window.jarvisBridge) {
          window.jarvisBridge.postError(args.join(' '));
        }
        oldErr.apply(console, arguments);
      };
    })();
  </script>
</head>
<body>
  $htmlContent
  <script>
    try {
      $jsContent
    } catch(err) {
      console.error("Runtime error: " + err.message);
    }
  </script>
</body>
</html>
""".trimIndent()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Project Selector & Actions Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Project Switcher
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisSurfaceVariant)
                        .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { projectDropdownExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Project",
                        tint = JarvisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activeProject?.name ?: "No Project",
                        color = JarvisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = projectDropdownExpanded,
                    onDismissRequest = { projectDropdownExpanded = false }
                ) {
                    allProjects.forEach { proj ->
                        DropdownMenuItem(
                            text = { Text(proj.name) },
                            onClick = {
                                viewModel.setActiveProject(proj)
                                projectDropdownExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("+ New Project", color = JarvisCyan) },
                        onClick = {
                            viewModel.createNewProject("Project ${System.currentTimeMillis() % 1000}")
                            projectDropdownExpanded = false
                        }
                    )
                }
            }

            // Action Buttons: Save, Run, Share
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Save Button
                IconButton(
                    onClick = {
                        viewModel.updateActiveProjectCode(htmlContent, cssContent, jsContent)
                        consoleLogs.add("Project saved successfully.")
                    },
                    modifier = Modifier.testTag("code_save_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save",
                        tint = JarvisCyan
                    )
                }

                // Run/Refresh Button
                IconButton(
                    onClick = {
                        reloadTrigger++
                        selectedTab = CodeEditorTab.PREVIEW
                        consoleLogs.add("Reloading preview...")
                    },
                    modifier = Modifier.testTag("code_run_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Preview",
                        tint = JarvisSuccess
                    )
                }

                // Share Button
                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, activeProject?.name ?: "Web Project")
                            putExtra(Intent.EXTRA_TEXT, fullHtml)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Web Project"))
                    },
                    modifier = Modifier.testTag("code_share_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = JarvisTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tabs: PREVIEW, HTML, CSS, JS, CONSOLE
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = JarvisSurface,
            contentColor = JarvisCyan,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = JarvisCyan
                )
            }
        ) {
            CodeEditorTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedTab == tab) JarvisCyan else JarvisTextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurface)
                .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp))
        ) {
            when (selectedTab) {
                CodeEditorTab.PREVIEW -> {
                    // Sandboxed Local WebView
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("local_webview_preview"),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false

                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun postLog(msg: String) {
                                        post {
                                            consoleLogs.add("[LOG] $msg")
                                        }
                                    }

                                    @JavascriptInterface
                                    fun postError(err: String) {
                                        post {
                                            consoleLogs.add("[ERR] $err")
                                        }
                                    }
                                }, "jarvisBridge")

                                webChromeClient = WebChromeClient()
                                webViewClient = WebViewClient()
                                loadDataWithBaseURL("https://jarvis.internal/", fullHtml, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL("https://jarvis.internal/", fullHtml, "text/html", "UTF-8", null)
                        }
                    )
                }

                CodeEditorTab.HTML -> {
                    OutlinedTextField(
                        value = htmlContent,
                        onValueChange = { htmlContent = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .testTag("html_editor"),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = JarvisTextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }

                CodeEditorTab.CSS -> {
                    OutlinedTextField(
                        value = cssContent,
                        onValueChange = { cssContent = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .testTag("css_editor"),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = JarvisTextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }

                CodeEditorTab.JAVASCRIPT -> {
                    OutlinedTextField(
                        value = jsContent,
                        onValueChange = { jsContent = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .testTag("js_editor"),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = JarvisTextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }

                CodeEditorTab.CONSOLE -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        items(consoleLogs) { log ->
                            Text(
                                text = "> $log",
                                color = if (log.contains("[ERR]")) JarvisCrimson else JarvisCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
