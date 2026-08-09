import React, { useEffect, useRef, useState } from 'react';
import { ImagePlus, X } from 'lucide-react';
import toast from 'react-hot-toast';
import './ImageUploadField.css';

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_BYTES = 3 * 1024 * 1024;

/**
 * Replaces the old "paste an Image URL" text input everywhere a food item is created or
 * edited. Purely a staging area: picking a file only previews it client-side (via an
 * object URL) and hands the raw File up through `onFileChange` — nothing is actually
 * uploaded until the parent form saves, so cancelling the form (or the whole modal) never
 * touches the server, and a picked-then-removed image never gets uploaded at all.
 *
 * @param currentImageUrl the item's existing image (edit mode), shown until the admin picks
 *                         a replacement or removes it.
 * @param file             a newly picked File, previewed in place of currentImageUrl.
 * @param removed          true once the admin has explicitly removed the existing image —
 *                          distinct from "never had one", so the parent knows to call the
 *                          remove-image endpoint on save rather than doing nothing.
 * @param onFileChange     (file: File|null, removed: boolean) => void
 */
export default function ImageUploadField({ label = 'Food Image', currentImageUrl, file, removed, onFileChange }) {
  const inputRef = useRef(null);
  const [previewUrl, setPreviewUrl] = useState(null);

  useEffect(() => {
    if (!file) {
      setPreviewUrl(null);
      return undefined;
    }
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    // Only ever holds one object URL at a time — revoke the previous one as soon as it's
    // no longer displayed, rather than leaking it for the lifetime of the page.
    return () => URL.revokeObjectURL(url);
  }, [file]);

  const displayedImage = previewUrl || (!removed ? currentImageUrl : null);

  const handlePick = (e) => {
    const picked = e.target.files?.[0];
    e.target.value = ''; // lets the same file be re-picked later (e.g. after Remove)
    if (!picked) return;

    if (!ALLOWED_TYPES.includes(picked.type)) {
      toast.error('Please upload a JPG, PNG, or WEBP image.');
      return;
    }
    if (picked.size > MAX_BYTES) {
      toast.error('Image size must be less than 3 MB.');
      return;
    }
    onFileChange(picked, false);
  };

  const handleRemove = () => {
    onFileChange(null, true);
  };

  return (
    <div className="form-group image-upload-field">
      <label>{label}</label>
      {displayedImage ? (
        <div className="image-upload-preview">
          <img src={displayedImage} alt="" />
          <div className="image-upload-actions">
            <button type="button" className="btn-secondary" onClick={() => inputRef.current?.click()}>
              Change Image
            </button>
            <button type="button" className="btn-danger-outline" onClick={handleRemove}>
              <X size={14} /> Remove
            </button>
          </div>
        </div>
      ) : (
        <button type="button" className="btn-secondary image-upload-btn" onClick={() => inputRef.current?.click()}>
          <ImagePlus size={16} /> Upload Image
        </button>
      )}
      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        onChange={handlePick}
        hidden
      />
    </div>
  );
}
