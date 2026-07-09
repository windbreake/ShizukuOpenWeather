#define MyAppName "ShizukuOpenWeather"
#define MyAppPublisher "windbreake"
#define MyAppExeName "ShizukuWeatherDesktop.exe"
#ifndef AppVersion
  #define AppVersion "0.1.0"
#endif
#ifndef SourcePublishDir
  #define SourcePublishDir "..\publish"
#endif
#ifndef SourceIconFile
  #define SourceIconFile "..\assets\app-icon.ico"
#endif
#ifndef OutputBaseDir
  #define OutputBaseDir "."
#endif
#ifndef OutputBaseFilename
  #define OutputBaseFilename "ShizukuOpenWeather-Setup"
#endif

[Setup]
AppId={{B9F8F6BC-36AE-4E61-95D9-B1BDAE7E8D7E}
AppName={#MyAppName}
AppVersion={#AppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={localappdata}\Programs\ShizukuOpenWeather
DefaultGroupName={#MyAppName}
UninstallDisplayIcon={app}\{#MyAppExeName}
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=lowest
OutputDir={#OutputBaseDir}
OutputBaseFilename={#OutputBaseFilename}
SetupIconFile={#SourceIconFile}
WizardStyle=modern
Compression=lzma2
SolidCompression=yes
DisableProgramGroupPage=yes
SetupLogging=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "{#SourcePublishDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon; IconFilename: "{app}\{#MyAppExeName}"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Launch {#MyAppName}"; Flags: nowait postinstall skipifsilent

