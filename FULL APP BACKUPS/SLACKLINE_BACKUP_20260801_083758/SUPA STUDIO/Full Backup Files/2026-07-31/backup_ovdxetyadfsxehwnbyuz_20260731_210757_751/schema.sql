-- Slackline Admin — Full Backup (executable schema)
-- Project: ovdxetyadfsxehwnbyuz
-- Generated: 2026-07-31T21:07:57.932529Z
-- NOTE: Run this against an empty database to restore schema, then load data/*.json per table.

CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "supabase_vault";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


CREATE TABLE IF NOT EXISTS "public"."app_demo_entitlements" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "user_id" uuid NOT NULL,
  "app_code" text NOT NULL,
  "device_code" text NOT NULL,
  "device_fingerprint_hash" text,
  "status" text NOT NULL DEFAULT 'active'::text,
  "demo_started_at" timestamptz NOT NULL DEFAULT timezone('utc'::text, now()),
  "demo_expires_at" timestamptz NOT NULL,
  "last_verified_at" timestamptz,
  "metadata" jsonb NOT NULL DEFAULT '{}'::jsonb,
  "created_at" timestamptz NOT NULL DEFAULT timezone('utc'::text, now()),
  "updated_at" timestamptz NOT NULL DEFAULT timezone('utc'::text, now())
);
ALTER TABLE "public"."app_demo_entitlements" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."app_ratings" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "app_id" uuid,
  "user_id" uuid,
  "stars" int4,
  "review" text DEFAULT ''::text,
  "created_at" timestamptz DEFAULT now()
);
ALTER TABLE "public"."app_ratings" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."apps" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "name" text NOT NULL,
  "tagline" text,
  "description" text,
  "version" text NOT NULL,
  "package_name" text,
  "category" text DEFAULT 'Utility'::text,
  "apk_url" text,
  "icon_url" text,
  "download_count" int4 DEFAULT 0,
  "is_published" bool DEFAULT false,
  "requires_license" bool DEFAULT false,
  "created_at" timestamptz DEFAULT now(),
  "updated_at" timestamptz DEFAULT now(),
  "screenshots" text,
  "is_featured" bool DEFAULT false,
  "has_update" bool DEFAULT false,
  "is_under_maintenance" bool DEFAULT false,
  "pricing" jsonb DEFAULT '[]'::jsonb,
  "sort_order" int4 DEFAULT 0,
  "features" jsonb,
  "sync_to_website" bool DEFAULT false,
  "version_code" int4
);
ALTER TABLE "public"."apps" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."backups" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "user_id" uuid NOT NULL,
  "device_id" uuid,
  "app_code" text NOT NULL,
  "backup_name" text NOT NULL,
  "storage_path" text NOT NULL,
  "version" text,
  "backup_size" int8 NOT NULL DEFAULT 0,
  "checksum" text,
  "is_auto" bool NOT NULL DEFAULT false,
  "notes" text,
  "metadata" jsonb NOT NULL DEFAULT '{}'::jsonb,
  "created_at" timestamptz NOT NULL DEFAULT timezone('utc'::text, now())
);
ALTER TABLE "public"."backups" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."broadcasts" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "created_at" timestamptz NOT NULL DEFAULT timezone('utc'::text, now()),
  "title" text NOT NULL,
  "message" text NOT NULL
);
ALTER TABLE "public"."broadcasts" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."client_requests" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "created_at" timestamptz NOT NULL DEFAULT timezone('utc'::text, now()),
  "name" text NOT NULL,
  "email" text NOT NULL,
  "whatsapp" text,
  "category" text NOT NULL,
  "description" text NOT NULL,
  "status" text NOT NULL DEFAULT 'pending'::text
);
ALTER TABLE "public"."client_requests" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."contact_info" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "whatsapp" text DEFAULT ''::text,
  "email" text DEFAULT ''::text,
  "discord" text DEFAULT ''::text,
  "telegram" text DEFAULT ''::text,
  "instagram" text DEFAULT ''::text,
  "website" text DEFAULT ''::text,
  "updated_at" timestamptz DEFAULT now()
);
ALTER TABLE "public"."contact_info" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."device_tokens" (
  "user_id" uuid NOT NULL,
  "token" text NOT NULL,
  "updated_at" timestamptz DEFAULT now(),
  "device_model" text DEFAULT ''::text
);
ALTER TABLE "public"."device_tokens" ADD PRIMARY KEY (user_id);

CREATE TABLE IF NOT EXISTS "public"."devices" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "user_id" uuid NOT NULL,
  "app_code" text NOT NULL,
  "device_name" text NOT NULL,
  "device_fingerprint_hash" text,
  "platform" text,
  "last_seen_at" timestamptz,
  "metadata" jsonb NOT NULL DEFAULT '{}'::jsonb,
  "created_at" timestamptz NOT NULL DEFAULT timezone('utc'::text, now()),
  "updated_at" timestamptz NOT NULL DEFAULT timezone('utc'::text, now())
);
ALTER TABLE "public"."devices" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."kl_demo_sessions" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "product_id" text NOT NULL,
  "device_code" text NOT NULL,
  "demo_started_at" timestamptz DEFAULT now(),
  "demo_expires_at" timestamptz,
  "is_active" bool DEFAULT true
);
ALTER TABLE "public"."kl_demo_sessions" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."kl_licenses" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "product_id" text NOT NULL,
  "license_id" text NOT NULL,
  "tier" text NOT NULL DEFAULT 'pro'::text,
  "device_code" text NOT NULL,
  "customer_name" text DEFAULT ''::text,
  "customer_email" text DEFAULT ''::text,
  "issued_at" timestamptz DEFAULT now(),
  "expires_at" timestamptz,
  "status" text NOT NULL DEFAULT 'active'::text,
  "revoked_at" timestamptz
);
ALTER TABLE "public"."kl_licenses" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."kl_products" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "product_id" text NOT NULL,
  "display_name" text NOT NULL,
  "prefix" text NOT NULL,
  "bundle_app" text NOT NULL,
  "created_at" timestamptz DEFAULT now()
);
ALTER TABLE "public"."kl_products" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."kl_revocations" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "product_id" text NOT NULL,
  "payload" jsonb NOT NULL DEFAULT '{}'::jsonb,
  "signature" text NOT NULL DEFAULT ''::text,
  "updated_at" timestamptz DEFAULT now()
);
ALTER TABLE "public"."kl_revocations" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."news" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "title" text NOT NULL,
  "body" text NOT NULL,
  "cover_image_url" text,
  "is_published" bool DEFAULT false,
  "created_at" timestamptz DEFAULT now()
);
ALTER TABLE "public"."news" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."products" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "name" text NOT NULL,
  "slug" text NOT NULL,
  "tagline" text,
  "description" text,
  "icon_url" text,
  "banner_url" text,
  "features" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "screenshots" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "pros" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "video_url" text,
  "pricing_type" text NOT NULL DEFAULT 'free'::text,
  "price_label" text,
  "pricing_tiers" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "min_android" text,
  "app_version" text,
  "app_size" text,
  "app_code" text,
  "tags" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "store_url" text,
  "download_url" text,
  "status" text NOT NULL DEFAULT 'live'::text,
  "is_featured" bool NOT NULL DEFAULT false,
  "sort_order" int4 NOT NULL DEFAULT 0,
  "created_at" timestamptz NOT NULL DEFAULT timezone('utc'::text, now()),
  "updated_at" timestamptz NOT NULL DEFAULT timezone('utc'::text, now())
);
ALTER TABLE "public"."products" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."profiles" (
  "id" uuid NOT NULL,
  "email" text,
  "display_name" text,
  "plan" text NOT NULL DEFAULT 'Standard'::text,
  "account_status" text NOT NULL DEFAULT 'active'::text,
  "created_at" timestamptz NOT NULL DEFAULT timezone('utc'::text, now()),
  "updated_at" timestamptz NOT NULL DEFAULT timezone('utc'::text, now()),
  "avatar_url" text,
  "is_admin" bool DEFAULT false
);
ALTER TABLE "public"."profiles" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."shv_admin_cloud_backups" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "user_id" uuid NOT NULL,
  "app_code" text NOT NULL DEFAULT 'sh_vertex_admin_panel'::text,
  "backup_name" text NOT NULL,
  "bundle_type" text NOT NULL DEFAULT 'app_data_backup'::text,
  "blob_text" text NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE "public"."shv_admin_cloud_backups" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."shv_admin_emergency_vault" (
  "id" uuid NOT NULL,
  "user_id" uuid NOT NULL,
  "category" text NOT NULL DEFAULT 'General'::text,
  "title" text NOT NULL,
  "value" text NOT NULL DEFAULT ''::text,
  "notes" text NOT NULL DEFAULT ''::text,
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  "created_at" timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE "public"."shv_admin_emergency_vault" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."site_downloads" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "code" text NOT NULL,
  "url" text NOT NULL,
  "label" text NOT NULL,
  "version" text,
  "updated_at" timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE "public"."site_downloads" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."site_settings" (
  "key" text NOT NULL,
  "value" text NOT NULL,
  "updated_at" timestamptz NOT NULL DEFAULT timezone('utc'::text, now())
);
ALTER TABLE "public"."site_settings" ADD PRIMARY KEY (key);

CREATE TABLE IF NOT EXISTS "public"."store_config" (
  "id" int4 NOT NULL DEFAULT 1,
  "latest_version" text NOT NULL,
  "apk_url" text NOT NULL,
  "update_message" text
);
ALTER TABLE "public"."store_config" ADD PRIMARY KEY (id);

CREATE TABLE IF NOT EXISTS "public"."store_devices" (
  "device_id" text NOT NULL,
  "user_id" uuid,
  "is_logged_in" bool NOT NULL DEFAULT false,
  "account_email" text,
  "device_model" text,
  "manufacturer" text,
  "os_version" text,
  "store_app_version" text,
  "installed_app_ids" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "first_seen_at" timestamptz NOT NULL DEFAULT now(),
  "last_seen_at" timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE "public"."store_devices" ADD PRIMARY KEY (device_id);

CREATE TABLE IF NOT EXISTS "public"."upcoming_updates" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "app_name" text NOT NULL,
  "title" text NOT NULL,
  "description" text,
  "target_version" text,
  "status" text DEFAULT 'planned'::text,
  "expected_date" text,
  "created_at" timestamptz DEFAULT now()
);
ALTER TABLE "public"."upcoming_updates" ADD PRIMARY KEY (id);

ALTER TABLE "public"."backups" ADD FOREIGN KEY ("device_id") REFERENCES "public"."devices"("id");
ALTER TABLE "public"."kl_licenses" ADD FOREIGN KEY ("product_id") REFERENCES "public"."kl_products"("product_id");
ALTER TABLE "public"."kl_revocations" ADD FOREIGN KEY ("product_id") REFERENCES "public"."kl_products"("product_id");
ALTER TABLE "public"."kl_demo_sessions" ADD FOREIGN KEY ("product_id") REFERENCES "public"."kl_products"("product_id");
ALTER TABLE "public"."app_ratings" ADD FOREIGN KEY ("app_id") REFERENCES "public"."apps"("id");

CREATE UNIQUE INDEX backups_storage_path_key ON public.backups USING btree (storage_path);
CREATE INDEX idx_devices_user_id ON public.devices USING btree (user_id);
CREATE INDEX idx_backups_user_id_created_at ON public.backups USING btree (user_id, created_at DESC);
CREATE INDEX idx_backups_device_id ON public.backups USING btree (device_id);
CREATE UNIQUE INDEX app_demo_entitlements_user_app_uq ON public.app_demo_entitlements USING btree (user_id, app_code);
CREATE UNIQUE INDEX app_demo_entitlements_device_app_uq ON public.app_demo_entitlements USING btree (device_code, app_code);
CREATE UNIQUE INDEX app_demo_entitlements_fingerprint_app_uq ON public.app_demo_entitlements USING btree (device_fingerprint_hash, app_code) WHERE (device_fingerprint_hash IS NOT NULL);
CREATE INDEX app_demo_entitlements_lookup_idx ON public.app_demo_entitlements USING btree (app_code, status, demo_expires_at);
CREATE UNIQUE INDEX site_downloads_code_key ON public.site_downloads USING btree (code);
CREATE UNIQUE INDEX kl_products_product_id_key ON public.kl_products USING btree (product_id);
CREATE UNIQUE INDEX kl_licenses_license_id_key ON public.kl_licenses USING btree (license_id);
CREATE INDEX idx_licenses_product ON public.kl_licenses USING btree (product_id);
CREATE INDEX idx_licenses_device ON public.kl_licenses USING btree (device_code);
CREATE UNIQUE INDEX kl_revocations_product_id_key ON public.kl_revocations USING btree (product_id);
CREATE UNIQUE INDEX kl_demo_sessions_product_id_device_code_key ON public.kl_demo_sessions USING btree (product_id, device_code);
CREATE INDEX idx_demo_product ON public.kl_demo_sessions USING btree (product_id);
CREATE INDEX idx_demo_device ON public.kl_demo_sessions USING btree (device_code);
CREATE UNIQUE INDEX products_slug_key ON public.products USING btree (slug);
CREATE INDEX idx_products_status ON public.products USING btree (status);
CREATE INDEX idx_products_sort ON public.products USING btree (sort_order, created_at);
CREATE INDEX idx_products_slug ON public.products USING btree (slug);
CREATE UNIQUE INDEX app_ratings_app_id_user_id_key ON public.app_ratings USING btree (app_id, user_id);

ALTER TABLE "public"."broadcasts" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."app_demo_entitlements" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."site_downloads" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."store_devices" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."site_settings" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."client_requests" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."kl_products" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."contact_info" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."apps" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."device_tokens" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."upcoming_updates" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."news" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."kl_revocations" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."kl_demo_sessions" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."kl_licenses" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."products" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."shv_admin_cloud_backups" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."backups" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."profiles" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."devices" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."shv_admin_emergency_vault" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."app_ratings" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."store_config" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."flow_state" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."saml_providers" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."instances" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."schema_migrations" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."refresh_tokens" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."users" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."audit_log_entries" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."sso_domains" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."mfa_amr_claims" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."identities" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."one_time_tokens" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."mfa_challenges" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."sso_providers" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."mfa_factors" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."saml_relay_states" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "auth"."sessions" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "storage"."objects" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "storage"."buckets" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "storage"."buckets_analytics" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "storage"."vector_indexes" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "storage"."buckets_vectors" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "storage"."s3_multipart_uploads_parts" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "storage"."s3_multipart_uploads" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "storage"."migrations" ENABLE ROW LEVEL SECURITY;
CREATE POLICY "profiles_select_own" ON "public"."profiles" FOR SELECT TO authenticated USING ((id = auth.uid()));
CREATE POLICY "profiles_insert_own" ON "public"."profiles" FOR INSERT TO authenticated WITH CHECK ((id = auth.uid()));
CREATE POLICY "profiles_update_own" ON "public"."profiles" FOR UPDATE TO authenticated USING ((id = auth.uid())) WITH CHECK ((id = auth.uid()));
CREATE POLICY "devices_select_own" ON "public"."devices" FOR SELECT TO authenticated USING ((user_id = auth.uid()));
CREATE POLICY "devices_insert_own" ON "public"."devices" FOR INSERT TO authenticated WITH CHECK ((user_id = auth.uid()));
CREATE POLICY "devices_update_own" ON "public"."devices" FOR UPDATE TO authenticated USING ((user_id = auth.uid())) WITH CHECK ((user_id = auth.uid()));
CREATE POLICY "devices_delete_own" ON "public"."devices" FOR DELETE TO authenticated USING ((user_id = auth.uid()));
CREATE POLICY "backups_select_own" ON "public"."backups" FOR SELECT TO authenticated USING ((user_id = auth.uid()));
CREATE POLICY "backups_insert_own" ON "public"."backups" FOR INSERT TO authenticated WITH CHECK ((user_id = auth.uid()));
CREATE POLICY "backups_update_own" ON "public"."backups" FOR UPDATE TO authenticated USING ((user_id = auth.uid())) WITH CHECK ((user_id = auth.uid()));
CREATE POLICY "backups_delete_own" ON "public"."backups" FOR DELETE TO authenticated USING ((user_id = auth.uid()));
CREATE POLICY "Enable read access for all" ON "public"."broadcasts" FOR SELECT TO public USING (true);
CREATE POLICY "Enable insert for authenticated" ON "public"."broadcasts" FOR INSERT TO public WITH CHECK ((auth.role() = 'authenticated'::text));
CREATE POLICY "backup_objects_select_own" ON "storage"."objects" FOR SELECT TO authenticated USING (((bucket_id = 'app-backups'::text) AND ((storage.foldername(name))[1] = (auth.uid())::text)));
CREATE POLICY "backup_objects_insert_own" ON "storage"."objects" FOR INSERT TO authenticated WITH CHECK (((bucket_id = 'app-backups'::text) AND ((storage.foldername(name))[1] = (auth.uid())::text)));
CREATE POLICY "backup_objects_update_own" ON "storage"."objects" FOR UPDATE TO authenticated USING (((bucket_id = 'app-backups'::text) AND ((storage.foldername(name))[1] = (auth.uid())::text))) WITH CHECK (((bucket_id = 'app-backups'::text) AND ((storage.foldername(name))[1] = (auth.uid())::text)));
CREATE POLICY "backup_objects_delete_own" ON "storage"."objects" FOR DELETE TO authenticated USING (((bucket_id = 'app-backups'::text) AND ((storage.foldername(name))[1] = (auth.uid())::text)));
CREATE POLICY "Users manage own admin backups" ON "public"."shv_admin_cloud_backups" FOR ALL TO public USING ((auth.uid() = user_id)) WITH CHECK ((auth.uid() = user_id));
CREATE POLICY "Users manage own emergency vault rows" ON "public"."shv_admin_emergency_vault" FOR ALL TO public USING ((auth.uid() = user_id)) WITH CHECK ((auth.uid() = user_id));
CREATE POLICY "read own demo entitlement" ON "public"."app_demo_entitlements" FOR SELECT TO authenticated USING ((( SELECT auth.uid() AS uid) = user_id));
CREATE POLICY "Public can read published apps" ON "public"."apps" FOR SELECT TO public USING ((is_published = true));
CREATE POLICY "Admin full access on apps" ON "public"."apps" FOR ALL TO public USING ((EXISTS ( SELECT 1
   FROM profiles
  WHERE ((profiles.id = auth.uid()) AND (profiles.is_admin = true)))));
CREATE POLICY "Public can read published news" ON "public"."news" FOR SELECT TO public USING ((is_published = true));
CREATE POLICY "Admin full access on news" ON "public"."news" FOR ALL TO public USING ((EXISTS ( SELECT 1
   FROM profiles
  WHERE ((profiles.id = auth.uid()) AND (profiles.is_admin = true)))));
CREATE POLICY "Public can read updates" ON "public"."upcoming_updates" FOR SELECT TO authenticated USING (true);
CREATE POLICY "Admin full access on updates" ON "public"."upcoming_updates" FOR ALL TO public USING ((EXISTS ( SELECT 1
   FROM profiles
  WHERE ((profiles.id = auth.uid()) AND (profiles.is_admin = true)))));
CREATE POLICY "Public read" ON "public"."contact_info" FOR SELECT TO public USING (true);
CREATE POLICY "Auth full access" ON "public"."contact_info" FOR ALL TO public USING ((auth.role() = 'authenticated'::text));
CREATE POLICY "Enable read for all" ON "public"."store_config" FOR SELECT TO public USING (true);
CREATE POLICY "Enable update for admin" ON "public"."store_config" FOR ALL TO public USING ((auth.role() = 'authenticated'::text));
CREATE POLICY "Enable delete for authenticated users" ON "public"."broadcasts" FOR DELETE TO authenticated USING (true);
CREATE POLICY "Anyone can read downloads" ON "public"."site_downloads" FOR SELECT TO public USING (true);
CREATE POLICY "Authenticated can update downloads" ON "public"."site_downloads" FOR UPDATE TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Public read revocations" ON "public"."kl_revocations" FOR SELECT TO public USING (true);
CREATE POLICY "Public read demo sessions" ON "public"."kl_demo_sessions" FOR SELECT TO public USING (true);
CREATE POLICY "products_public_read" ON "public"."products" FOR SELECT TO public USING ((status = ANY (ARRAY['live'::text, 'coming-soon'::text])));
CREATE POLICY "products_admin_all" ON "public"."products" FOR ALL TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Users can read all ratings" ON "public"."app_ratings" FOR SELECT TO public USING (true);
CREATE POLICY "Users can insert own rating" ON "public"."app_ratings" FOR INSERT TO public WITH CHECK ((auth.uid() = user_id));
CREATE POLICY "Users can update own rating" ON "public"."app_ratings" FOR UPDATE TO public USING ((auth.uid() = user_id));
CREATE POLICY "Users manage own token" ON "public"."device_tokens" FOR ALL TO public USING ((auth.uid() = user_id)) WITH CHECK ((auth.uid() = user_id));
CREATE POLICY "settings_public_read" ON "public"."site_settings" FOR SELECT TO public USING (true);
CREATE POLICY "settings_admin_write" ON "public"."site_settings" FOR ALL TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "anon_read" ON "public"."kl_products" FOR SELECT TO anon USING (true);
CREATE POLICY "anon_read" ON "public"."kl_licenses" FOR SELECT TO anon USING (true);
CREATE POLICY "anon_read" ON "public"."kl_revocations" FOR SELECT TO anon USING (true);
CREATE POLICY "auth_insert" ON "public"."kl_demo_sessions" FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "auth_read" ON "public"."kl_demo_sessions" FOR SELECT TO authenticated USING (true);
CREATE POLICY "Allow public inserts" ON "public"."client_requests" FOR INSERT TO anon WITH CHECK (true);
CREATE POLICY "Allow app reads" ON "public"."client_requests" FOR SELECT TO anon USING (true);
CREATE POLICY "Allow app updates" ON "public"."client_requests" FOR UPDATE TO anon USING (true);
CREATE POLICY "store devices upsert own heartbeat" ON "public"."store_devices" FOR INSERT TO anon,authenticated WITH CHECK (true);
CREATE POLICY "store devices update own heartbeat" ON "public"."store_devices" FOR UPDATE TO anon,authenticated USING (true) WITH CHECK (true);
CREATE POLICY "store devices admin read" ON "public"."store_devices" FOR SELECT TO authenticated USING ((EXISTS ( SELECT 1
   FROM profiles
  WHERE ((profiles.id = auth.uid()) AND (profiles.is_admin = true)))));

CREATE OR REPLACE FUNCTION public.handle_new_user()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
                                                                begin
                                                                  insert into public.profiles (id, email, display_name, plan, account_status)
                                                                    values (
                                                                        new.id,
                                                                            new.email,
                                                                                coalesce(new.raw_user_meta_data ->> 'display_name', split_part(coalesce(new.email, ''), '@', 1), 'SH Vertex Customer'),
                                                                                    'Standard',
                                                                                        'active'
                                                                                          )
                                                                                            on conflict (id) do update
                                                                                                set email = excluded.email,
                                                                                                        display_name = coalesce(public.profiles.display_name, excluded.display_name),
                                                                                                                updated_at = timezone('utc', now());
                                                                                                                  return new;
                                                                                                                  end;
                                                                                                                  $function$
;

CREATE OR REPLACE FUNCTION public.increment_download_count(app_id_input uuid)
 RETURNS void
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
                                                                                                                                                    UPDATE public.apps
                                                                                                                                                        SET download_count = download_count + 1
                                                                                                                                                            WHERE id = app_id_input;
                                                                                                                                                            $function$
;

CREATE OR REPLACE FUNCTION public.rls_auto_enable()
 RETURNS event_trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'pg_catalog'
AS $function$
DECLARE
  cmd record;
BEGIN
  FOR cmd IN
    SELECT *
    FROM pg_event_trigger_ddl_commands()
    WHERE command_tag IN ('CREATE TABLE', 'CREATE TABLE AS', 'SELECT INTO')
      AND object_type IN ('table','partitioned table')
  LOOP
     IF cmd.schema_name IS NOT NULL AND cmd.schema_name IN ('public') AND cmd.schema_name NOT IN ('pg_catalog','information_schema') AND cmd.schema_name NOT LIKE 'pg_toast%' AND cmd.schema_name NOT LIKE 'pg_temp%' THEN
      BEGIN
        EXECUTE format('alter table if exists %s enable row level security', cmd.object_identity);
        RAISE LOG 'rls_auto_enable: enabled RLS on %', cmd.object_identity;
      EXCEPTION
        WHEN OTHERS THEN
          RAISE LOG 'rls_auto_enable: failed to enable RLS on %', cmd.object_identity;
      END;
     ELSE
        RAISE LOG 'rls_auto_enable: skip % (either system schema or not in enforced list: %.)', cmd.object_identity, cmd.schema_name;
     END IF;
  END LOOP;
END;
$function$
;

CREATE OR REPLACE FUNCTION public.set_app_demo_entitlements_updated_at()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                            begin
                              new.updated_at = timezone('utc', now());
                                return new;
                                end;
                                $function$
;

CREATE OR REPLACE FUNCTION public.set_updated_at()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
begin
  new.updated_at = timezone('utc', now());
    return new;
    end;
    $function$
;

CREATE OR REPLACE FUNCTION public.verify_or_start_trial(p_app_code text, p_device_fingerprint text, p_device_code text, p_demo_hours integer)
 RETURNS json
 LANGUAGE plpgsql
 SECURITY DEFINER
AS $function$
          DECLARE
            v_user_id uuid;
              v_entitlement record;
              BEGIN
                -- 1. Get the calling user's ID securely from their login token
                  v_user_id := auth.uid();
                    IF v_user_id IS NULL THEN
                        RETURN json_build_object('valid', false, 'message', 'Unauthorized. Please log in.');
                          END IF;

                            -- 2. Check if this DEVICE has already claimed a trial under ANY account
                              SELECT * INTO v_entitlement 
                                FROM public.app_demo_entitlements 
                                  WHERE app_code = p_app_code AND device_fingerprint_hash = p_device_fingerprint;

                                    IF FOUND THEN
                                        -- It has been used on this device. Is it the same user?
                                            IF v_entitlement.user_id != v_user_id THEN
                                                  RETURN json_build_object('valid', false, 'message', 'This device has already claimed a trial with another account.');
                                                      END IF;

                                                          -- Same user. Is the trial expired?
                                                              IF v_entitlement.demo_expires_at > now() THEN
                                                                    RETURN json_build_object('valid', true, 'message', 'Trial active.', 'expires_at', v_entitlement.demo_expires_at);
                                                                        ELSE
                                                                              RETURN json_build_object('valid', false, 'message', 'Trial expired. Please activate a Pro key.');
                                                                                  END IF;
                                                                                    END IF;

                                                                                      -- 3. Check if this USER has already claimed a trial on ANY OTHER device
                                                                                        SELECT * INTO v_entitlement 
                                                                                          FROM public.app_demo_entitlements 
                                                                                            WHERE app_code = p_app_code AND user_id = v_user_id;

                                                                                              IF FOUND THEN
                                                                                                   RETURN json_build_object('valid', false, 'message', 'This account has already claimed a trial on another device.');
                                                                                                     END IF;

                                                                                                       -- 4. If we reach here, it is a brand new user and a brand new device. Start Trial!
                                                                                                         INSERT INTO public.app_demo_entitlements (
                                                                                                             user_id, app_code, device_code, device_fingerprint_hash, status, demo_started_at, demo_expires_at, updated_at
                                                                                                               ) VALUES (
                                                                                                                   v_user_id, p_app_code, p_device_code, p_device_fingerprint, 'active', now(), now() + (p_demo_hours || ' hours')::interval, now()
                                                                                                                     ) RETURNING * INTO v_entitlement;

                                                                                                                       RETURN json_build_object('valid', true, 'message', 'Trial started successfully!', 'expires_at', v_entitlement.demo_expires_at);
                                                                                                                       END;
                                                                                                                       $function$
;

CREATE TRIGGER trg_profiles_updated_at BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_devices_updated_at BEFORE UPDATE ON public.devices FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_app_demo_entitlements_updated_at BEFORE UPDATE ON public.app_demo_entitlements FOR EACH ROW EXECUTE FUNCTION set_app_demo_entitlements_updated_at();
CREATE TRIGGER trg_products_updated_at BEFORE UPDATE ON public.products FOR EACH ROW EXECUTE FUNCTION set_updated_at();
