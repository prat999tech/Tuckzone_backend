import React from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { ImagePlus, X } from 'lucide-react-native';
import Toast from 'react-native-toast-message';
import { colors, radius, spacing, typography } from '../theme';
import type { PickedImage } from '../api/admin';

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_BYTES = 3 * 1024 * 1024;

interface Props {
  label?: string;
  /** The item's existing photo (edit mode), shown until a replacement is picked or removed. */
  currentImageUrl?: string | null;
  /** A newly picked image, previewed in place of currentImageUrl. */
  image: PickedImage | null;
  /** True once the existing image has been explicitly removed — distinct from "never had
   *  one", so the caller knows to call the remove-image endpoint on save. */
  removed: boolean;
  onChange: (image: PickedImage | null, removed: boolean) => void;
}

/**
 * Replaces the old "paste an Image URL" text field in menu item forms. Picking an image
 * only stages it locally (nothing is uploaded until the parent form saves) — see the web
 * equivalent, ImageUploadField.jsx, for the same contract.
 */
export function ImagePickerField({ label = 'Food Image', currentImageUrl, image, removed, onChange }: Props) {
  async function pick() {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) {
      Toast.show({ type: 'error', text1: 'Photo library access is needed to upload an image' });
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      quality: 0.8,
    });
    if (result.canceled || !result.assets?.[0]) return;

    const asset = result.assets[0];
    if (asset.mimeType && !ALLOWED_TYPES.includes(asset.mimeType)) {
      Toast.show({ type: 'error', text1: 'Please upload a JPG, PNG, or WEBP image.' });
      return;
    }
    if (asset.fileSize && asset.fileSize > MAX_BYTES) {
      Toast.show({ type: 'error', text1: 'Image size must be less than 3 MB.' });
      return;
    }
    onChange({ uri: asset.uri, fileName: asset.fileName, mimeType: asset.mimeType }, false);
  }

  const displayedUri = image?.uri || (!removed ? currentImageUrl : null);

  return (
    <View style={styles.container}>
      <Text style={styles.label}>{label}</Text>
      {displayedUri ? (
        <View style={styles.previewRow}>
          <Image source={{ uri: displayedUri }} style={styles.preview} />
          <View style={{ gap: spacing.sm }}>
            <Pressable style={styles.actionBtn} onPress={pick} hitSlop={8}>
              <Text style={styles.actionText}>Change Image</Text>
            </Pressable>
            <Pressable style={styles.actionBtn} onPress={() => onChange(null, true)} hitSlop={8}>
              <X size={14} color={colors.danger} />
              <Text style={[styles.actionText, styles.removeText]}>Remove</Text>
            </Pressable>
          </View>
        </View>
      ) : (
        <Pressable style={styles.uploadBtn} onPress={pick}>
          <ImagePlus size={16} color={colors.primary} />
          <Text style={styles.actionText}>Upload Image</Text>
        </Pressable>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { marginBottom: spacing.md },
  label: { ...typography.label, marginBottom: spacing.xs },
  previewRow: { flexDirection: 'row', gap: spacing.md, alignItems: 'center' },
  preview: { width: 84, height: 84, borderRadius: radius.md, backgroundColor: colors.borderLight },
  actionBtn: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs, paddingVertical: spacing.xs },
  actionText: { ...typography.bodySmall, fontWeight: '600', color: colors.primary },
  removeText: { color: colors.danger },
  uploadBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.lg,
    borderRadius: radius.DEFAULT,
    borderWidth: 1,
    borderColor: colors.border,
    alignSelf: 'flex-start',
  },
});
