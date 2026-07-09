using Microsoft.Web.WebView2.Core;

namespace ShizukuWeatherDesktop;

public partial class Form1 : Form
{
    private LocalWeatherHost? _host;

    public Form1()
    {
        InitializeComponent();
    }

    protected override async void OnShown(EventArgs e)
    {
        base.OnShown(e);
        await LaunchAsync();
    }

    protected override async void OnFormClosed(FormClosedEventArgs e)
    {
        if (_host is not null)
        {
            await _host.DisposeAsync();
        }

        base.OnFormClosed(e);
    }

    private async Task LaunchAsync()
    {
        try
        {
            statusLabel.Text = "正在启动桌面天气...";
            _host = await LocalWeatherHost.StartAsync();

            await weatherView.EnsureCoreWebView2Async();
            ConfigureWebView(weatherView.CoreWebView2);

            weatherView.Source = _host.BaseUri;
            statusLabel.Text = "正在载入天气面板...";
        }
        catch (Exception error)
        {
            statusLabel.Text = $"桌面程序启动失败: {error.Message}";
            statusLabel.Visible = true;
        }
    }

    private static void ConfigureWebView(CoreWebView2? coreWebView)
    {
        if (coreWebView is null)
        {
            return;
        }

        coreWebView.Settings.AreDefaultContextMenusEnabled = false;
        coreWebView.Settings.IsStatusBarEnabled = false;
        coreWebView.Settings.AreBrowserAcceleratorKeysEnabled = true;
        coreWebView.Settings.AreDevToolsEnabled = false;
    }

    private void weatherView_NavigationCompleted(object? sender, CoreWebView2NavigationCompletedEventArgs e)
    {
        if (e.IsSuccess)
        {
            statusLabel.Visible = false;
            return;
        }

        statusLabel.Text = $"载入失败: {e.WebErrorStatus}";
        statusLabel.Visible = true;
    }


}

