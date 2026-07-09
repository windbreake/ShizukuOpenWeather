#nullable enable

using Microsoft.Web.WebView2.WinForms;

namespace ShizukuWeatherDesktop;

partial class Form1
{
    private System.ComponentModel.IContainer? components = null;
    private Panel chromeBar = null!;
    private Label titleLabel = null!;
    private Label statusLabel = null!;
    private WebView2 weatherView = null!;

    protected override void Dispose(bool disposing)
    {
        if (disposing)
        {
            components?.Dispose();
        }

        base.Dispose(disposing);
    }

    private void InitializeComponent()
    {
        components = new System.ComponentModel.Container();
        chromeBar = new Panel();
        titleLabel = new Label();
        statusLabel = new Label();
        weatherView = new WebView2();
        chromeBar.SuspendLayout();
        ((System.ComponentModel.ISupportInitialize)weatherView).BeginInit();
        SuspendLayout();
        // 
        // chromeBar
        // 
        chromeBar.BackColor = Color.FromArgb(232, 240, 252);
        chromeBar.Controls.Add(titleLabel);
        chromeBar.Dock = DockStyle.Top;
        chromeBar.Location = new Point(0, 0);
        chromeBar.Name = "chromeBar";
        chromeBar.Padding = new Padding(18, 0, 18, 0);
        chromeBar.Size = new Size(1560, 44);
        chromeBar.TabIndex = 0;
        // 
        // titleLabel
        // 
        titleLabel.AutoSize = true;
        titleLabel.Font = new Font("Microsoft YaHei UI", 10.5F, FontStyle.Bold, GraphicsUnit.Point, 134);
        titleLabel.ForeColor = Color.FromArgb(19, 34, 55);
        titleLabel.Location = new Point(18, 12);
        titleLabel.Name = "titleLabel";
        titleLabel.Size = new Size(178, 19);
        titleLabel.TabIndex = 0;
        titleLabel.Text = "Shizuku Open Weather";

        // 
        // statusLabel
        // 
        statusLabel.AutoSize = true;
        statusLabel.BackColor = Color.Transparent;
        statusLabel.Font = new Font("Microsoft YaHei UI", 11F, FontStyle.Regular, GraphicsUnit.Point, 134);
        statusLabel.ForeColor = Color.FromArgb(19, 34, 55);
        statusLabel.Location = new Point(24, 60);
        statusLabel.Name = "statusLabel";
        statusLabel.Size = new Size(129, 20);
        statusLabel.TabIndex = 2;
        statusLabel.Text = "正在准备天气桌面…";
        // 
        // weatherView
        // 
        weatherView.AllowExternalDrop = false;
        weatherView.CreationProperties = null;
        weatherView.DefaultBackgroundColor = Color.White;
        weatherView.Dock = DockStyle.Fill;
        weatherView.Location = new Point(0, 44);
        weatherView.Name = "weatherView";
        weatherView.Size = new Size(1560, 916);
        weatherView.Source = new Uri("about:blank", UriKind.Absolute);
        weatherView.TabIndex = 1;
        weatherView.ZoomFactor = 1D;
        weatherView.NavigationCompleted += weatherView_NavigationCompleted;
        // 
        // Form1
        // 
        AutoScaleDimensions = new SizeF(7F, 17F);
        AutoScaleMode = AutoScaleMode.Font;
        BackColor = Color.FromArgb(244, 248, 255);
        ClientSize = new Size(1560, 960);
        Controls.Add(statusLabel);
        Controls.Add(weatherView);
        Controls.Add(chromeBar);
        MinimumSize = new Size(1280, 780);
        Name = "Form1";
        StartPosition = FormStartPosition.CenterScreen;
        Text = "Shizuku Open Weather";
        chromeBar.ResumeLayout(false);
        chromeBar.PerformLayout();
        ((System.ComponentModel.ISupportInitialize)weatherView).EndInit();
        ResumeLayout(false);
        PerformLayout();
    }
}



