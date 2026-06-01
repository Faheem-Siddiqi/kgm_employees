package com.kgm.ui.component;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

final class WindowsNativeFolderDialog {
    private WindowsNativeFolderDialog() {
    }

    static File[] chooseFolders(Component parent, String title) {
        if (!isWindows()) {
            return new File[0];
        }

        Path script = null;
        try {
            script = Files.createTempFile("kgm-folder-picker-", ".ps1");
            Files.writeString(script, pickerScript(), Charset.defaultCharset());

            Process process = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-STA",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    script.toAbsolutePath().toString(),
                    title == null || title.isBlank() ? "Select folders" : title,
                    ownerTitle(parent)
            ).redirectError(ProcessBuilder.Redirect.DISCARD).start();

            List<File> folders = new ArrayList<>();
            try (BufferedReader reader = process.inputReader(Charset.defaultCharset())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        folders.add(new File(line.trim()));
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return folders.toArray(File[]::new);
            }
        } catch (IOException exception) {
            return new File[0];
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new File[0];
        } finally {
            if (script != null) {
                try {
                    Files.deleteIfExists(script);
                } catch (IOException ignored) {
                }
            }
        }

        return new File[0];
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("win");
    }

    private static String pickerScript() {
        return """
                param([string]$Title, [string]$OwnerTitle)
                Add-Type -TypeDefinition @'
                using System;
                using System.Collections.Generic;
                using System.Runtime.InteropServices;

                class User32 {
                    [DllImport("user32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
                    public static extern IntPtr FindWindow(string lpClassName, string lpWindowName);

                    [DllImport("user32.dll")]
                    public static extern bool SetForegroundWindow(IntPtr hWnd);
                }

                [ComImport, Guid("DC1C5A9C-E88A-4DDE-A5A1-60F82A20AEF7")]
                class FileOpenDialog {
                }

                [Flags]
                enum FOS : uint {
                    FOS_OVERWRITEPROMPT = 0x2,
                    FOS_STRICTFILETYPES = 0x4,
                    FOS_NOCHANGEDIR = 0x8,
                    FOS_PICKFOLDERS = 0x20,
                    FOS_FORCEFILESYSTEM = 0x40,
                    FOS_ALLNONSTORAGEITEMS = 0x80,
                    FOS_NOVALIDATE = 0x100,
                    FOS_ALLOWMULTISELECT = 0x200,
                    FOS_PATHMUSTEXIST = 0x800,
                    FOS_FILEMUSTEXIST = 0x1000,
                    FOS_CREATEPROMPT = 0x2000,
                    FOS_SHAREAWARE = 0x4000,
                    FOS_NOREADONLYRETURN = 0x8000,
                    FOS_NOTESTFILECREATE = 0x10000,
                    FOS_HIDEMRUPLACES = 0x20000,
                    FOS_HIDEPINNEDPLACES = 0x40000,
                    FOS_NODEREFERENCELINKS = 0x100000,
                    FOS_OKBUTTONNEEDSINTERACTION = 0x200000,
                    FOS_DONTADDTORECENT = 0x2000000,
                    FOS_FORCESHOWHIDDEN = 0x10000000,
                    FOS_DEFAULTNOMINIMODE = 0x20000000,
                    FOS_FORCEPREVIEWPANEON = 0x40000000
                }

                enum SIGDN : uint {
                    SIGDN_FILESYSPATH = 0x80058000
                }

                [ComImport, Guid("42f85136-db7e-439c-85f1-e4075d135fc8"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
                interface IFileDialog {
                    [PreserveSig] int Show(IntPtr parent);
                    void SetFileTypes(uint cFileTypes, IntPtr rgFilterSpec);
                    void SetFileTypeIndex(uint iFileType);
                    void GetFileTypeIndex(out uint piFileType);
                    void Advise(IntPtr pfde, out uint pdwCookie);
                    void Unadvise(uint dwCookie);
                    void SetOptions(FOS fos);
                    void GetOptions(out FOS pfos);
                    void SetDefaultFolder(IShellItem psi);
                    void SetFolder(IShellItem psi);
                    void GetFolder(out IShellItem ppsi);
                    void GetCurrentSelection(out IShellItem ppsi);
                    void SetFileName([MarshalAs(UnmanagedType.LPWStr)] string pszName);
                    void GetFileName([MarshalAs(UnmanagedType.LPWStr)] out string pszName);
                    void SetTitle([MarshalAs(UnmanagedType.LPWStr)] string pszTitle);
                    void SetOkButtonLabel([MarshalAs(UnmanagedType.LPWStr)] string pszText);
                    void SetFileNameLabel([MarshalAs(UnmanagedType.LPWStr)] string pszLabel);
                    void GetResult(out IShellItem ppsi);
                    void AddPlace(IShellItem psi, int fdap);
                    void SetDefaultExtension([MarshalAs(UnmanagedType.LPWStr)] string pszDefaultExtension);
                    void Close(int hr);
                    void SetClientGuid(ref Guid guid);
                    void ClearClientData();
                    void SetFilter(IntPtr pFilter);
                }

                [ComImport, Guid("d57c7288-d4ad-4768-be02-9d969532d960"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
                interface IFileOpenDialog {
                    [PreserveSig] int Show(IntPtr parent);
                    void SetFileTypes(uint cFileTypes, IntPtr rgFilterSpec);
                    void SetFileTypeIndex(uint iFileType);
                    void GetFileTypeIndex(out uint piFileType);
                    void Advise(IntPtr pfde, out uint pdwCookie);
                    void Unadvise(uint dwCookie);
                    void SetOptions(FOS fos);
                    void GetOptions(out FOS pfos);
                    void SetDefaultFolder(IShellItem psi);
                    void SetFolder(IShellItem psi);
                    void GetFolder(out IShellItem ppsi);
                    void GetCurrentSelection(out IShellItem ppsi);
                    void SetFileName([MarshalAs(UnmanagedType.LPWStr)] string pszName);
                    void GetFileName([MarshalAs(UnmanagedType.LPWStr)] out string pszName);
                    void SetTitle([MarshalAs(UnmanagedType.LPWStr)] string pszTitle);
                    void SetOkButtonLabel([MarshalAs(UnmanagedType.LPWStr)] string pszText);
                    void SetFileNameLabel([MarshalAs(UnmanagedType.LPWStr)] string pszLabel);
                    void GetResult(out IShellItem ppsi);
                    void AddPlace(IShellItem psi, int fdap);
                    void SetDefaultExtension([MarshalAs(UnmanagedType.LPWStr)] string pszDefaultExtension);
                    void Close(int hr);
                    void SetClientGuid(ref Guid guid);
                    void ClearClientData();
                    void SetFilter(IntPtr pFilter);
                    void GetResults(out IShellItemArray ppenum);
                    void GetSelectedItems(out IShellItemArray ppsai);
                }

                [ComImport, Guid("43826d1e-e718-42ee-bc55-a1e261c37bfe"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
                interface IShellItem {
                    void BindToHandler(IntPtr pbc, ref Guid bhid, ref Guid riid, out IntPtr ppv);
                    void GetParent(out IShellItem ppsi);
                    void GetDisplayName(SIGDN sigdnName, out IntPtr ppszName);
                    void GetAttributes(uint sfgaoMask, out uint psfgaoAttribs);
                    void Compare(IShellItem psi, uint hint, out int piOrder);
                }

                [ComImport, Guid("b63ea76d-1f85-456f-a19c-48159efa858b"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
                interface IShellItemArray {
                    void BindToHandler(IntPtr pbc, ref Guid bhid, ref Guid riid, out IntPtr ppvOut);
                    void GetPropertyStore(int flags, ref Guid riid, out IntPtr ppv);
                    void GetPropertyDescriptionList(ref Guid keyType, ref Guid riid, out IntPtr ppv);
                    void GetAttributes(uint attribFlags, uint sfgaoMask, out uint psfgaoAttribs);
                    void GetCount(out uint pdwNumItems);
                    void GetItemAt(uint dwIndex, out IShellItem ppsi);
                }

                public static class NativeFolderPicker {
                    public static string[] PickFolders(string title, string ownerTitle) {
                        IFileOpenDialog dialog = (IFileOpenDialog)new FileOpenDialog();
                        FOS options;
                        dialog.GetOptions(out options);
                        dialog.SetOptions(options
                            | FOS.FOS_PICKFOLDERS
                            | FOS.FOS_ALLOWMULTISELECT
                            | FOS.FOS_FORCEFILESYSTEM
                            | FOS.FOS_PATHMUSTEXIST
                            | FOS.FOS_NOCHANGEDIR);
                        dialog.SetTitle(title);

                        IntPtr owner = IntPtr.Zero;
                        if (!String.IsNullOrWhiteSpace(ownerTitle)) {
                            owner = User32.FindWindow(null, ownerTitle);
                            if (owner != IntPtr.Zero) {
                                User32.SetForegroundWindow(owner);
                            }
                        }

                        int hr = dialog.Show(owner);
                        if (hr == unchecked((int)0x800704C7)) {
                            return new string[0];
                        }
                        if (hr < 0) {
                            Marshal.ThrowExceptionForHR(hr);
                        }

                        IShellItemArray results;
                        dialog.GetResults(out results);
                        uint count;
                        results.GetCount(out count);
                        List<string> folders = new List<string>();
                        for (uint index = 0; index < count; index++) {
                            IShellItem item;
                            results.GetItemAt(index, out item);
                            IntPtr pathPointer;
                            item.GetDisplayName(SIGDN.SIGDN_FILESYSPATH, out pathPointer);
                            string path = Marshal.PtrToStringUni(pathPointer);
                            Marshal.FreeCoTaskMem(pathPointer);
                            if (!String.IsNullOrWhiteSpace(path)) {
                                folders.Add(path);
                            }
                        }
                        return folders.ToArray();
                    }
                }
                '@

                [NativeFolderPicker]::PickFolders($Title, $OwnerTitle) | ForEach-Object { Write-Output $_ }
                """;
    }

    private static String ownerTitle(Component parent) {
        Window window = parent instanceof Window
                ? (Window) parent
                : parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (window instanceof Frame frame && frame.getTitle() != null && !frame.getTitle().isBlank()) {
            return frame.getTitle();
        }
        if (window instanceof Dialog dialog && dialog.getTitle() != null && !dialog.getTitle().isBlank()) {
            return dialog.getTitle();
        }
        return "";
    }
}
