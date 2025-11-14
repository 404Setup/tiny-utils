package one.pkg.tinyutils.sounds;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VolumeControl {
    private int currentMixerIndex = -1;

    /**
     * Get all available volume service information
     */
    public List<VolumeServiceInfo> getAllVolumeServices() {
        List<VolumeServiceInfo> services = new ArrayList<>();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

        for (int i = 0; i < mixerInfos.length; i++) {
            Mixer.Info info = mixerInfos[i];
            boolean supportsVolume = checkVolumeSupport(info);

            VolumeServiceInfo serviceInfo = new VolumeServiceInfo(
                    info.getName(),
                    info.getDescription(),
                    info.getVendor(),
                    info.getVersion(),
                    i,
                    supportsVolume
            );
            services.add(serviceInfo);
        }

        return services;
    }

    /**
     * Check if the mixer supports volume control
     */
    private boolean checkVolumeSupport(Mixer.Info mixerInfo) {
        try {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] sourceLines = mixer.getSourceLineInfo();
            Line.Info[] targetLines = mixer.getTargetLineInfo();

            for (Line.Info lineInfo : sourceLines) {
                if (hasVolumeControl(mixer, lineInfo)) {
                    return true;
                }
            }

            for (Line.Info lineInfo : targetLines) {
                if (hasVolumeControl(mixer, lineInfo)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Check if the line has volume control
     */
    private boolean hasVolumeControl(Mixer mixer, Line.Info lineInfo) {
        try {
            Line line = mixer.getLine(lineInfo);
            if (!line.isOpen()) {
                line.open();
            }

            boolean hasControl = line.isControlSupported(FloatControl.Type.MASTER_GAIN) ||
                    line.isControlSupported(FloatControl.Type.VOLUME);

            if (!line.isOpen()) {
                line.close();
            }

            return hasControl;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the volume level for all available volume services
     */
    public List<VolumeInfo> getAllVolumes() {
        List<VolumeInfo> volumes = new ArrayList<>();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixerInfos) {
            Optional<VolumeInfo> volumeInfo = getVolumeForMixer(mixerInfo);
            volumeInfo.ifPresent(volumes::add);
        }

        return volumes;
    }

    /**
     * Gets volume information for the specified mixer
     */
    private Optional<VolumeInfo> getVolumeForMixer(Mixer.Info mixerInfo) {
        try {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);

            for (Line.Info lineInfo : mixer.getSourceLineInfo()) {
                Optional<VolumeInfo> volume = getVolumeFromLine(mixer, lineInfo);
                if (volume.isPresent()) {
                    return volume;
                }
            }

            for (Line.Info lineInfo : mixer.getTargetLineInfo()) {
                Optional<VolumeInfo> volume = getVolumeFromLine(mixer, lineInfo);
                if (volume.isPresent()) {
                    return volume;
                }
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    /**
     * Gets volume information from the line
     */
    private Optional<VolumeInfo> getVolumeFromLine(Mixer mixer, Line.Info lineInfo) {
        Line line = null;
        try {
            line = mixer.getLine(lineInfo);
            boolean needClose = false;

            if (!line.isOpen()) {
                line.open();
                needClose = true;
            }

            FloatControl volumeControl = null;
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            } else if (line.isControlSupported(FloatControl.Type.VOLUME)) {
                volumeControl = (FloatControl) line.getControl(FloatControl.Type.VOLUME);
            }

            if (volumeControl != null) {
                float currentDb = volumeControl.getValue();
                float minDb = volumeControl.getMinimum();
                float maxDb = volumeControl.getMaximum();

                float normalizedVolume = (currentDb - minDb) / (maxDb - minDb);
                normalizedVolume = Math.max(0.0f, Math.min(1.0f, normalizedVolume));

                boolean muted = false;
                if (line.isControlSupported(BooleanControl.Type.MUTE)) {
                    BooleanControl muteControl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
                    muted = muteControl.getValue();
                }

                if (needClose) {
                    line.close();
                }

                return Optional.of(new VolumeInfo(normalizedVolume, currentDb, minDb, maxDb, muted));
            }

            if (needClose) {
                line.close();
            }
        } catch (Exception e) {
            if (line != null && line.isOpen()) {
                line.close();
            }
        }
        return Optional.empty();
    }

    /**
     * Sets the volume level for all available volume services
     *
     * @param volume volume value, range from 0.0 to 1.0
     * @return number of services successfully set
     */
    public int setAllVolumes(float volume) {
        if (volume < 0.0f || volume > 1.0f) {
            throw new IllegalArgumentException("Volume must be between 0.0 and 1.0");
        }

        int successCount = 0;
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixerInfos) {
            if (setVolumeForMixer(mixerInfo, volume)) {
                successCount++;
            }
        }

        return successCount;
    }

    /**
     * Sets the volume level for the specified mixer
     */
    private boolean setVolumeForMixer(Mixer.Info mixerInfo, float volume) {
        try {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            boolean success = false;

            for (Line.Info lineInfo : mixer.getSourceLineInfo()) {
                if (setVolumeForLine(mixer, lineInfo, volume)) {
                    success = true;
                }
            }

            for (Line.Info lineInfo : mixer.getTargetLineInfo()) {
                if (setVolumeForLine(mixer, lineInfo, volume)) {
                    success = true;
                }
            }

            return success;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sets the volume level for the line
     */
    private boolean setVolumeForLine(Mixer mixer, Line.Info lineInfo, float volume) {
        Line line = null;
        try {
            line = mixer.getLine(lineInfo);
            boolean needClose = false;

            if (!line.isOpen()) {
                line.open();
                needClose = true;
            }

            FloatControl volumeControl = null;
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            } else if (line.isControlSupported(FloatControl.Type.VOLUME)) {
                volumeControl = (FloatControl) line.getControl(FloatControl.Type.VOLUME);
            }

            if (volumeControl != null) {
                float minDb = volumeControl.getMinimum();
                float maxDb = volumeControl.getMaximum();
                float targetDb = minDb + (maxDb - minDb) * volume;
                volumeControl.setValue(targetDb);

                if (needClose) {
                    line.close();
                }
                return true;
            }

            if (needClose) {
                line.close();
            }
        } catch (Exception e) {
            if (line != null && line.isOpen()) {
                line.close();
            }
        }
        return false;
    }

    public Optional<VolumeServiceInfo> getCurrentVolumeService() {
        if (currentMixerIndex == -1) {
            List<VolumeServiceInfo> services = getAllVolumeServices();
            for (VolumeServiceInfo service : services) {
                if (service.supportsVolumeControl()) {
                    currentMixerIndex = service.index();
                    return Optional.of(service);
                }
            }
            return Optional.empty();
        }

        List<VolumeServiceInfo> services = getAllVolumeServices();
        if (currentMixerIndex >= 0 && currentMixerIndex < services.size()) {
            return Optional.of(services.get(currentMixerIndex));
        }

        return Optional.empty();
    }

    /**
     * Get the volume level of the current volume service
     */
    public Optional<VolumeInfo> getCurrentVolume() {
        Optional<VolumeServiceInfo> currentService = getCurrentVolumeService();
        if (currentService.isEmpty()) {
            return Optional.empty();
        }

        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        int index = currentService.get().index();

        if (index >= 0 && index < mixerInfos.length) {
            return getVolumeForMixer(mixerInfos[index]);
        }

        return Optional.empty();
    }

    /**
     * Sets the volume level for the current volume service
     *
     * @param volume volume level, range from 0.0 to 1.0
     * @return whether the setting was successful
     */
    public boolean setCurrentVolume(float volume) {
        if (volume < 0.0f || volume > 1.0f)
            throw new IllegalArgumentException("Volume must be between 0.0 and 1.0");

        Optional<VolumeServiceInfo> currentService = getCurrentVolumeService();
        if (currentService.isEmpty()) {
            return false;
        }

        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        int index = currentService.get().index();

        if (index >= 0 && index < mixerInfos.length) {
            return setVolumeForMixer(mixerInfos[index], volume);
        }

        return false;
    }

    /**
     * Switch the current volume service to another volume service.
     * Note: This only changes the mixer used by this wrapper class, not the default audio device of the operating system.
     *
     * @param serviceIndex service index
     * @return whether the switch succeeded
     */
    public boolean switchToVolumeService(int serviceIndex) {
        List<VolumeServiceInfo> services = getAllVolumeServices();

        if (serviceIndex < 0 || serviceIndex >= services.size()) {
            return false;
        }

        VolumeServiceInfo targetService = services.get(serviceIndex);
        if (!targetService.supportsVolumeControl()) {
            return false;
        }

        currentMixerIndex = serviceIndex;
        return true;
    }

    /**
     * Switch volume service by name
     *
     * @param serviceName service name
     * @return whether the switch succeeded
     */
    public boolean switchToVolumeServiceByName(String serviceName) {
        List<VolumeServiceInfo> services = getAllVolumeServices();

        for (VolumeServiceInfo service : services) {
            if (service.name().equals(serviceName) && service.supportsVolumeControl()) {
                currentMixerIndex = service.index();
                return true;
            }
        }

        return false;
    }

    public void resetCurrentService() {
        currentMixerIndex = -1;
    }

    public record VolumeServiceInfo(String name, String description, String vendor, String version, int index,
                                    boolean supportsVolumeControl) {
    }

    public record VolumeInfo(float volume, float volumeDb, float minDb, float maxDb, boolean muted) {
    }
}