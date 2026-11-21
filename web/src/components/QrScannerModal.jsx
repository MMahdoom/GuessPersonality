import React, { useEffect, useRef, useState } from 'react';
import jsQR from 'jsqr';
import { Camera, ImageUp, X } from 'lucide-react';
import { extractRoomCodeFromScan } from '../utils/gameUtils.js';

export default function QrScannerModal({ onDetected, onClose }) {
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);
  const rafRef = useRef(null);
  const handledRef = useRef(false);

  const [cameraError, setCameraError] = useState('');
  const [statusText, setStatusText] = useState('Starting camera...');

  const stopCamera = () => {
    if (rafRef.current) {
      cancelAnimationFrame(rafRef.current);
      rafRef.current = null;
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
    }
  };

  const handleRawDecode = (rawValue) => {
    if (handledRef.current || !rawValue) return false;
    const roomCode = extractRoomCodeFromScan(rawValue);
    if (roomCode) {
      handledRef.current = true;
      stopCamera();
      onDetected(roomCode);
      return true;
    }
    setStatusText('QR detected, but it does not contain a valid room code.');
    return false;
  };

  useEffect(() => {
    let active = true;

    async function start() {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        setCameraError(
          'Live camera scanning requires HTTPS and camera support. You can also scan a QR photo below.'
        );
        return;
      }

      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: { ideal: 'environment' },
            width: { ideal: 1280 },
            height: { ideal: 720 },
          },
          audio: false,
        });

        if (!active) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }

        streamRef.current = stream;
        const video = videoRef.current;
        if (video) {
          video.srcObject = stream;
          await video.play();
        }
        setStatusText('Point your camera at the host’s QR code');

        // Use native BarcodeDetector when available, with jsQR canvas fallback for iOS Safari/Firefox
        let detector = null;
        if ('BarcodeDetector' in window) {
          try {
            detector = new window.BarcodeDetector({ formats: ['qr_code'] });
          } catch {
            detector = null;
          }
        }

        let lastScanTime = 0;
        const scanLoop = async (timestamp) => {
          if (!active || handledRef.current) return;
          const v = videoRef.current;
          const canvas = canvasRef.current;

          if (v && v.readyState >= 2 && timestamp - lastScanTime >= 140) {
            lastScanTime = timestamp;

            if (detector) {
              try {
                const codes = await detector.detect(v);
                if (codes.length > 0 && handleRawDecode(codes[0].rawValue)) {
                  return;
                }
              } catch {
                detector = null; // Fall back to jsQR cleanly
              }
            }

            if (canvas && v.videoWidth > 0 && v.videoHeight > 0) {
              const targetW = Math.min(640, v.videoWidth);
              const scale = targetW / v.videoWidth;
              const targetH = Math.round(v.videoHeight * scale);
              canvas.width = targetW;
              canvas.height = targetH;
              const ctx = canvas.getContext('2d', { willReadFrequently: true });
              if (ctx) {
                ctx.drawImage(v, 0, 0, targetW, targetH);
                const imageData = ctx.getImageData(0, 0, targetW, targetH);
                const code = jsQR(imageData.data, imageData.width, imageData.height, {
                  inversionAttempts: 'dontInvert',
                });
                if (code?.data && handleRawDecode(code.data)) {
                  return;
                }
              }
            }
          }

          rafRef.current = requestAnimationFrame(scanLoop);
        };

        rafRef.current = requestAnimationFrame(scanLoop);
      } catch (err) {
        if (!active) return;
        const denied = err?.name === 'NotAllowedError' || err?.name === 'PermissionDeniedError';
        setCameraError(
          denied
            ? 'Camera permission was denied. Allow camera access in browser settings or upload a QR photo below.'
            : 'Could not open camera on this device. You can upload a QR photo or type the room code manually.'
        );
      }
    }

    start();
    return () => {
      active = false;
      stopCamera();
    };
  }, []);

  const handleImageUpload = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const img = new Image();
    const objectUrl = URL.createObjectURL(file);
    img.onload = () => {
      try {
        const canvas = canvasRef.current || document.createElement('canvas');
        const maxDim = 1024;
        const scale = Math.min(1, maxDim / Math.max(img.width, img.height));
        canvas.width = Math.round(img.width * scale);
        canvas.height = Math.round(img.height * scale);
        const ctx = canvas.getContext('2d', { willReadFrequently: true });
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const code = jsQR(imageData.data, imageData.width, imageData.height);
        if (code?.data) {
          if (!handleRawDecode(code.data)) {
            setCameraError('QR code found, but it is not a Guess the Personality room link.');
          }
        } else {
          setCameraError('No QR code detected in that photo. Try a clearer image or type the code.');
        }
      } finally {
        URL.revokeObjectURL(objectUrl);
      }
    };
    img.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      setCameraError('Could not read the selected image.');
    };
    img.src = objectUrl;
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-label="Scan Room QR Code"
    >
      <div className="w-full max-w-md overflow-hidden rounded-3xl bg-[#120C21] text-white shadow-2xl border border-amber-400/30">
        <div className="flex items-center justify-between border-b border-white/10 px-5 py-3.5">
          <div className="flex items-center gap-2">
            <Camera className="h-5 w-5 text-amber-400" />
            <h2 className="font-display text-lg font-bold">Scan Room QR</h2>
          </div>
          <button
            onClick={() => {
              stopCamera();
              onClose();
            }}
            className="rounded-full p-1.5 text-purple-200 hover:bg-white/10"
            aria-label="Close scanner"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="p-4 space-y-3">
          {!cameraError ? (
            <div className="relative aspect-square w-full overflow-hidden rounded-2xl bg-black">
              <video
                ref={videoRef}
                playsInline
                muted
                className="h-full w-full object-cover"
              />
              {/* Scanning Frame Overlay */}
              <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
                <div className="relative h-56 w-56 rounded-2xl border-2 border-amber-400/80 shadow-[0_0_0_9999px_rgba(18,12,33,0.45)]">
                  <span className="absolute -left-0.5 -top-0.5 h-6 w-6 rounded-tl-xl border-l-4 border-t-4 border-amber-400" />
                  <span className="absolute -right-0.5 -top-0.5 h-6 w-6 rounded-tr-xl border-r-4 border-t-4 border-amber-400" />
                  <span className="absolute -bottom-0.5 -left-0.5 h-6 w-6 rounded-bl-xl border-b-4 border-l-4 border-amber-400" />
                  <span className="absolute -bottom-0.5 -right-0.5 h-6 w-6 rounded-br-xl border-b-4 border-r-4 border-amber-400" />
                </div>
              </div>
            </div>
          ) : (
            <div className="rounded-2xl border border-rose-400/40 bg-rose-950/60 p-4 text-sm text-rose-100">
              {cameraError}
            </div>
          )}

          <p className="text-center text-xs text-purple-200/85">{statusText}</p>

          <label className="flex w-full cursor-pointer items-center justify-center gap-2 rounded-xl border border-amber-400/50 bg-white/5 py-2.5 text-xs font-bold text-amber-300 hover:bg-white/10">
            <ImageUp className="h-4 w-4" />
            Scan QR from Photo / Gallery
            <input
              type="file"
              accept="image/*"
              onChange={handleImageUpload}
              className="hidden"
            />
          </label>
        </div>

        <canvas ref={canvasRef} className="hidden" />
      </div>
    </div>
  );
}
